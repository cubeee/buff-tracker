package com.github.cubeee.bufftracker.model;

import lombok.Getter;

/**
 * Where a buff's icon comes from: an item image, or a sprite such as a spell icon.
 */
@Getter
public final class BuffIcon {

    public enum Source {
        ITEM,
        SPRITE
    }

    private final Source source;
    private final int id;

    private BuffIcon(Source source, int id) {
        this.source = source;
        this.id = id;
    }

    public static BuffIcon item(int itemId) {
        return new BuffIcon(Source.ITEM, itemId);
    }

    public static BuffIcon sprite(int spriteId) {
        return new BuffIcon(Source.SPRITE, spriteId);
    }
}
