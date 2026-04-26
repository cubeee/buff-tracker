package com.github.cubeee.bufftracker.model;

import com.github.cubeee.bufftracker.config.DisplayMode;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

import java.util.*;

@Getter
public class BuffGroup {

    // Used for new groups and for saved groups missing a value
    public static final DisplayMode DEFAULT_DISPLAY_MODE = DisplayMode.ICON;
    public static final ComponentOrientation DEFAULT_ORIENTATION = ComponentOrientation.VERTICAL;
    public static final int MAX_NAME_LENGTH = 50;

    private final String id;
    @Setter
    private String name;
    @Setter
    private DisplayMode displayMode;
    @Setter
    private boolean isVisible;
    @Setter
    private boolean isReversed;
    @Setter
    private ComponentOrientation orientation;
    @Setter
    private boolean isMissingOnly;
    @Setter
    private boolean isFlashMissing;
    private final List<BuffWrapper> buffs;

    public BuffGroup(
            String id,
            String name,
            DisplayMode displayMode,
            boolean isVisible,
            boolean isReversed,
            ComponentOrientation orientation,
            List<BuffWrapper> buffs
    ) {
        this.id = id;
        this.name = name;
        this.displayMode = displayMode;
        this.isVisible = isVisible;
        this.isReversed = isReversed;
        this.orientation = orientation;
        this.buffs = new ArrayList<>(buffs);
    }

    public BuffGroup copy() {
        List<BuffWrapper> wrappers = new ArrayList<>(buffs.size());
        for (BuffWrapper wrapper : buffs) {
            wrappers.add(new BuffWrapper(wrapper.getBuff(), wrapper.isPresent()));
        }
        BuffGroup copy = new BuffGroup(id, name, displayMode, isVisible, isReversed, orientation, wrappers);
        copy.setMissingOnly(isMissingOnly);
        copy.setFlashMissing(isFlashMissing);
        return copy;
    }

    /**
     * Whether the groups differ in visibility only. Buff presence isn't compared.
     */
    public boolean equalsIgnoringVisibility(BuffGroup other) {
        return hasSameSettings(other) && hasSameBuffs(other);
    }

    private boolean hasSameSettings(BuffGroup other) {
        return id.equals(other.id)
                && Objects.equals(name, other.name)
                && displayMode == other.displayMode
                && isReversed == other.isReversed
                && orientation == other.orientation
                && isMissingOnly == other.isMissingOnly
                && isFlashMissing == other.isFlashMissing;
    }

    private boolean hasSameBuffs(BuffGroup other) {
        if (buffs.size() != other.buffs.size()) {
            return false;
        }
        for (int i = 0; i < buffs.size(); i++) {
            if (buffs.get(i).getBuff() != other.buffs.get(i).getBuff()) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Buff buff) {
        for (BuffWrapper wrapper : buffs) {
            if (wrapper.getBuff() == buff) {
                return true;
            }
        }
        return false;
    }

    public boolean isShowIcon() {
        return displayMode == DisplayMode.ICON || displayMode == DisplayMode.BOTH;
    }

    public boolean isShowText() {
        return displayMode == DisplayMode.TEXT || displayMode == DisplayMode.BOTH;
    }
}
