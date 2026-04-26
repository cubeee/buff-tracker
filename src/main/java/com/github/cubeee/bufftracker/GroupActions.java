package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.Buff;
import com.github.cubeee.bufftracker.model.BuffGroup;
import com.github.cubeee.bufftracker.model.BuffWrapper;
import com.github.cubeee.bufftracker.config.DisplayMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.*;

/**
 * Group changes requested by the panel. Called on the EDT; applied on the client thread, where groups are read.
 */
@Singleton
public class GroupActions {

    private final ClientThread clientThread;
    private final GroupStore groupStore;

    @Inject
    GroupActions(ClientThread clientThread, GroupStore groupStore) {
        this.clientThread = clientThread;
        this.groupStore = groupStore;
    }

    public void createGroup() {
        clientThread.invokeLater(groupStore::create);
    }

    public void deleteGroup(String id) {
        clientThread.invokeLater(() -> groupStore.delete(id));
    }

    public void setName(String id, String name) {
        modifyGroup(id, group -> group.setName(name));
    }

    public void setVisible(String id, boolean visible) {
        clientThread.invokeLater(() -> groupStore.setVisible(id, visible));
    }

    public void setDisplayMode(String id, DisplayMode mode) {
        modifyGroup(id, group -> group.setDisplayMode(mode));
    }

    public void setDirection(String id, ComponentOrientation orientation, boolean reversed) {
        modifyGroup(id, group -> {
            group.setOrientation(orientation);
            group.setReversed(reversed);
        });
    }

    public void setMissingOnly(String id, boolean missingOnly) {
        modifyGroup(id, group -> group.setMissingOnly(missingOnly));
    }

    public void setFlashMissing(String id, boolean flashMissing) {
        modifyGroup(id, group -> group.setFlashMissing(flashMissing));
    }

    public void addBuff(String id, Buff buff) {
        modifyGroup(id, group -> {
            if (!group.contains(buff)) {
                // GroupStore.modify() refreshes presence right after
                group.getBuffs().add(new BuffWrapper(buff, false));
            }
        });
    }

    public void removeBuff(String id, Buff buff) {
        modifyGroup(id, group -> group.getBuffs().removeIf(wrapper -> wrapper.getBuff() == buff));
    }

    public void moveBuff(String id, Buff buff, int delta) {
        modifyGroup(id, group -> {
            List<BuffWrapper> buffs = group.getBuffs();
            for (int i = 0; i < buffs.size(); i++) {
                if (buffs.get(i).getBuff() == buff) {
                    int target = i + delta;
                    if (target >= 0 && target < buffs.size()) {
                        Collections.swap(buffs, i, target);
                    }
                    return;
                }
            }
        });
    }

    private void modifyGroup(String id, Consumer<BuffGroup> modification) {
        clientThread.invokeLater(() -> groupStore.modify(id, modification));
    }
}
