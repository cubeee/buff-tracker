package com.github.cubeee.bufftracker.model;

import com.github.cubeee.bufftracker.config.DisplayMode;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

import java.util.*;

public class SavedGroup {

    String id;
    String name;
    DisplayMode displayMode;
    boolean visible;
    boolean reversed;
    ComponentOrientation orientation;
    boolean missingOnly;
    boolean flashMissing;
    List<String> buffs;

    public static SavedGroup fromGroup(BuffGroup group) {
        SavedGroup saved = new SavedGroup();
        saved.id = group.getId();
        saved.name = group.getName();
        saved.displayMode = group.getDisplayMode();
        saved.visible = group.isVisible();
        saved.reversed = group.isReversed();
        saved.orientation = group.getOrientation();
        saved.missingOnly = group.isMissingOnly();
        saved.flashMissing = group.isFlashMissing();
        saved.buffs = new ArrayList<>();
        for (BuffWrapper wrapper : group.getBuffs()) {
            saved.buffs.add(wrapper.getBuff().name());
        }
        return saved;
    }

    public BuffGroup toGroup() {
        BuffGroup group = new BuffGroup(
                // The id also names the overlay, so a group without one gets a fresh id and position
                id != null && !id.isEmpty() ? id : UUID.randomUUID().toString(),
                toName(),
                displayMode != null ? displayMode : BuffGroup.DEFAULT_DISPLAY_MODE,
                visible,
                reversed,
                orientation != null ? orientation : BuffGroup.DEFAULT_ORIENTATION,
                toWrappers()
        );
        group.setMissingOnly(missingOnly);
        group.setFlashMissing(flashMissing);
        return group;
    }

    // Hand-edited data may hold a name longer than the panel allows typing
    private String toName() {
        if (name == null) {
            return "Group";
        }
        return name.length() > BuffGroup.MAX_NAME_LENGTH ? name.substring(0, BuffGroup.MAX_NAME_LENGTH) : name;
    }

    private List<BuffWrapper> toWrappers() {
        List<BuffWrapper> wrappers = new ArrayList<>();
        if (buffs == null) {
            return wrappers;
        }
        for (String buffId : buffs) {
            Buff buff = Buff.fromId(buffId);
            // Skip buffs that have since been removed, and duplicates from hand-edited data
            if (buff != null && wrappers.stream().noneMatch(wrapper -> wrapper.getBuff() == buff)) {
                wrappers.add(new BuffWrapper(buff, false));
            }
        }
        return wrappers;
    }
}
