package com.github.cubeee.bufftracker.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BuffWrapper {

    private final Buff buff;

    @Setter
    private boolean isPresent;

    public BuffWrapper(Buff buff, boolean isPresent) {
        this.buff = buff;
        this.isPresent = isPresent;
    }
}
