package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.BuffGroup;
import com.github.cubeee.bufftracker.model.BuffWrapper;
import com.github.cubeee.bufftracker.model.SavedGroup;
import com.github.cubeee.bufftracker.config.ConfigKeys;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.*;

/**
 * The groups and their overlays. Used on the client thread only.
 */
@Slf4j
@Singleton
class GroupStore {

    private final ConfigManager configManager;
    private final Gson gson;
    private final OverlayManager overlayManager;
    private final GroupOverlayFactory overlayFactory;
    private final Client client;

    private final List<BuffGroup> groups = new ArrayList<>();
    private final Map<String, GroupOverlay> overlays = new HashMap<>();
    // Restored by toggleVisibility() after the visible group has been hidden
    private String lastVisibleId;
    // The groups JSON last read from or written to the config, to tell outside changes from our own
    private String savedJson;
    // The config profile the groups were loaded from
    private long loadedProfileId;
    private Runnable changeListener = () -> {};
    // Told when the groups were replaced wholesale, rather than changed
    private Runnable reloadListener = () -> {};

    @Inject
    GroupStore(
            ConfigManager configManager,
            Gson gson,
            OverlayManager overlayManager,
            GroupOverlayFactory overlayFactory,
            Client client
    ) {
        this.configManager = configManager;
        this.gson = gson;
        this.overlayManager = overlayManager;
        this.overlayFactory = overlayFactory;
        this.client = client;
    }

    void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    void setReloadListener(Runnable reloadListener) {
        this.reloadListener = reloadListener;
    }

    void load() {
        // A reload for a config change may come before the startup load
        unload();
        loadedProfileId = getProfileId();
        savedJson = configManager.getConfiguration(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME, ConfigKeys.GROUPS);
        groups.addAll(parseGroups(savedJson));

        // Only one group may be visible; keep the first if older data has several
        boolean visibleFound = false;
        for (BuffGroup group : groups) {
            if (group.isVisible() && visibleFound) {
                group.setVisible(false);
            }
            visibleFound |= group.isVisible();
        }

        for (BuffGroup group : groups) {
            refreshPresence(group);
            addOverlay(group);
        }
    }

    private List<BuffGroup> parseGroups(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        List<SavedGroup> saved;
        try {
            saved = gson.fromJson(json, new TypeToken<List<SavedGroup>>() {}.getType());
        } catch (JsonSyntaxException e) {
            log.warn("Failed to parse saved groups, ignoring them", e);
            return Collections.emptyList();
        }
        if (saved == null) {
            return Collections.emptyList();
        }

        List<BuffGroup> loaded = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (SavedGroup savedGroup : saved) {
            if (savedGroup == null) {
                continue;
            }
            BuffGroup group = savedGroup.toGroup();
            // Skip duplicate ids; they would share an overlay position
            if (ids.add(group.getId())) {
                loaded.add(group);
            }
        }
        return loaded;
    }

    void clear() {
        unload();
        changeListener = () -> {};
        reloadListener = () -> {};
    }

    /**
     * Replaces the groups with the ones saved in the current config profile.
     */
    void reload() {
        load();
        // Only the panel needs updating; saving would just write back what was read
        reloadListener.run();
    }

    /**
     * Reloads the groups if the saved ones differ from what was last read or written, i.e. they were changed by
     * something other than this store.
     */
    void reloadIfChanged() {
        if (getProfileId() != loadedProfileId) {
            // Switching profiles also posts this. ProfileChanged reloads the groups then; reloading here too would
            // treat the old profile's groups as deleted and reset their positions in the new profile.
            return;
        }
        String json = configManager.getConfiguration(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME, ConfigKeys.GROUPS);
        if (Objects.equals(json, savedJson)) {
            return;
        }
        Map<String, GroupOverlay> removed = new HashMap<>(overlays);
        reload();
        // Forget the positions of groups deleted elsewhere, e.g. by editing the config by hand, as delete() would
        removed.keySet().removeAll(overlays.keySet());
        for (GroupOverlay overlay : removed.values()) {
            overlayManager.resetOverlay(overlay);
        }
    }

    private long getProfileId() {
        ConfigProfile profile = configManager.getProfile();
        return profile != null ? profile.getId() : -1;
    }

    private void unload() {
        // Only the visible group's overlay is added; removing the others does nothing
        for (GroupOverlay overlay : overlays.values()) {
            overlayManager.remove(overlay);
        }
        overlays.clear();
        groups.clear();
        lastVisibleId = null;
    }

    // Called when the plugin config is reset; works whether or not the plugin is running
    void reset() {
        // Read from the config rather than the live overlays, which don't exist while the plugin is off.
        // Resetting an overlay that was never added just clears its saved position.
        for (BuffGroup group : parseGroups(configManager.getConfiguration(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME, ConfigKeys.GROUPS))) {
            overlayManager.resetOverlay(overlayFactory.create(group));
        }
        unload();
        changed();
    }

    List<BuffGroup> snapshot() {
        List<BuffGroup> snapshot = new ArrayList<>(groups.size());
        for (BuffGroup group : groups) {
            snapshot.add(group.copy());
        }
        return snapshot;
    }

    void create() {
        boolean visible = groups.stream().noneMatch(BuffGroup::isVisible);
        BuffGroup group = new BuffGroup(
                UUID.randomUUID().toString(),
                "New group",
                BuffGroup.DEFAULT_DISPLAY_MODE,
                visible,
                false,
                BuffGroup.DEFAULT_ORIENTATION,
                List.of()
        );
        groups.add(group);
        addOverlay(group);
        changed();
    }

    void delete(String id) {
        groups.removeIf(group -> group.getId().equals(id));
        GroupOverlay overlay = overlays.remove(id);
        if (overlay != null) {
            // Forget the saved position so it doesn't linger in the config
            overlayManager.resetOverlay(overlay);
            overlayManager.remove(overlay);
        }
        changed();
    }

    /**
     * Showing a group hides all others, as only one group may be visible at a time.
     */
    void setVisible(String id, boolean visible) {
        // A stale panel may still show a group that's gone, e.g. right after a profile switch
        int index = indexOf(id);
        if (index < 0 || groups.get(index).isVisible() == visible) {
            // The panel's eye button toggled itself when clicked; send the actual state back to it
            changeListener.run();
            return;
        }
        BuffGroup target = groups.get(index);
        // Only one group is ever visible: the target when hiding, or the one it replaces when showing
        BuffGroup previous = getVisibleGroup();
        if (previous != null) {
            lastVisibleId = previous.getId();
            previous.setVisible(false);
            overlayManager.remove(overlays.get(previous.getId()));
        }
        if (visible) {
            // Hidden groups aren't refreshed when vars change, so their state may be stale
            refreshPresence(target);
            target.setVisible(true);
            overlayManager.add(overlays.get(target.getId()));
        }
        changed();
    }

    void cycleVisible() {
        if (groups.isEmpty()) {
            return;
        }
        int current = indexOf(findVisibleId());
        if (current < 0) {
            // Nothing shown: continue from the group that was last visible
            current = indexOf(lastVisibleId);
        }
        BuffGroup next = groups.get((current + 1) % groups.size());
        setVisible(next.getId(), true);
    }

    void toggleVisibility() {
        String visibleId = findVisibleId();
        if (visibleId != null) {
            setVisible(visibleId, false);
            return;
        }
        if (groups.isEmpty()) {
            return;
        }
        String id = indexOf(lastVisibleId) >= 0 ? lastVisibleId : groups.get(0).getId();
        setVisible(id, true);
    }

    BuffGroup getVisibleGroup() {
        for (BuffGroup group : groups) {
            if (group.isVisible()) {
                return group;
            }
        }
        return null;
    }

    private String findVisibleId() {
        BuffGroup visible = getVisibleGroup();
        return visible != null ? visible.getId() : null;
    }

    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    void modify(String id, Consumer<BuffGroup> modification) {
        for (BuffGroup group : groups) {
            if (group.getId().equals(id)) {
                modification.accept(group);
                // Added buffs start out as missing
                refreshPresence(group);
                GroupOverlay overlay = overlays.get(id);
                if (overlay != null) {
                    overlay.invalidateLayout();
                }
                changed();
                return;
            }
        }
    }

    void refreshPresence(BuffGroup group) {
        if (group == null) {
            return;
        }
        for (BuffWrapper wrapper : group.getBuffs()) {
            wrapper.setPresent(wrapper.getBuff().isActive(client));
        }
    }

    // The font or icon size changed
    void invalidateLayouts() {
        for (GroupOverlay overlay : overlays.values()) {
            overlay.invalidateLayout();
        }
    }

    // Hidden groups draw nothing, so only the visible group's overlay is added to the overlay manager
    private void addOverlay(BuffGroup group) {
        GroupOverlay overlay = overlayFactory.create(group);
        overlays.put(group.getId(), overlay);
        if (group.isVisible()) {
            overlayManager.add(overlay);
        }
    }

    private void changed() {
        save();
        changeListener.run();
    }

    private void save() {
        // Set before writing, as the write posts a ConfigChanged that reloadIfChanged() compares against
        if (groups.isEmpty()) {
            savedJson = null;
            configManager.unsetConfiguration(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME, ConfigKeys.GROUPS);
            return;
        }
        List<SavedGroup> saved = new ArrayList<>(groups.size());
        for (BuffGroup group : groups) {
            saved.add(SavedGroup.fromGroup(group));
        }
        savedJson = gson.toJson(saved);
        configManager.setConfiguration(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME, ConfigKeys.GROUPS, savedJson);
    }
}
