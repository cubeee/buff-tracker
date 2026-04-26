package com.github.cubeee.bufftracker.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Scroll pane view that tracks the viewport width, so children shrink (and labels truncate) instead of
 * scrolling horizontally. The wrapped component sits at the top at its natural height.
 */
class ViewportWidthPanel extends JPanel implements Scrollable {

    ViewportWidthPanel(Component content) {
        super(new BorderLayout());
        add(content, BorderLayout.NORTH);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
