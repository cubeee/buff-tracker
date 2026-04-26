package com.github.cubeee.bufftracker.config;

public enum DisplayMode {
    ICON("Icon only"),
    TEXT("Text only"),
    BOTH("Icon and text");

    private final String label;

    DisplayMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
