package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.BuffGroup;
import com.github.cubeee.bufftracker.model.BuffWrapper;
import com.github.cubeee.bufftracker.overlay.BuffComponent;
import net.runelite.api.Client;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.Text;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.*;

public class GroupOverlay extends Overlay {

    private final BuffGroup group;
    private final BuffIconStore iconStore;
    private final BuffTrackerConfig config;
    private final RuneLiteConfig runeLiteConfig;
    private final TooltipManager tooltipManager;
    private final Client client;

    private static final int PADDING = 6;

    // Measured lazily on the client thread and kept until invalidateLayout()
    private Dimension boxSize;
    private Dimension fullSize;
    // One per member in group order, built with the layout; only positions, colors and images change between frames
    private final List<BuffComponent> components = new ArrayList<>();

    GroupOverlay(
            Plugin plugin,
            BuffGroup group,
            BuffIconStore iconStore,
            BuffTrackerConfig config,
            RuneLiteConfig runeLiteConfig,
            TooltipManager tooltipManager,
            Client client
    ) {
        super(plugin);
        this.group = group;
        this.iconStore = iconStore;
        this.config = config;
        this.runeLiteConfig = runeLiteConfig;
        this.tooltipManager = tooltipManager;
        this.client = client;

        setPosition(OverlayPosition.TOP_LEFT);
        setDragTargetable(false);
        setResizable(false);
    }

    @Override
    public String getName() {
        return "BuffTrackerGroup_" + group.getId();
    }

    @Override
    public Dimension getPreferredSize() {
        // Size is derived from the boxes; ignore any previously saved user size
        return null;
    }

    /**
     * Forgets the measured size; call when the group's members, display mode or orientation, or the font or icon
     * size change.
     */
    void invalidateLayout() {
        boxSize = null;
        fullSize = null;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<BuffWrapper> buffs = group.getBuffs();
        if (!group.isVisible() || buffs.isEmpty()) {
            return null;
        }

        // The size check guards against a member change that missed invalidateLayout()
        if (boxSize == null || components.size() != buffs.size()) {
            Font font = config.getFont().getFont();
            // Measure every member, not just the visible ones, so the overlay keeps the same size (and saved
            // position) when "Show missing only" hides boxes
            boxSize = measureBoxSize(graphics.getFontMetrics(font));
            fullSize = getFullSize(buffs.size(), boxSize);
            createComponents(buffs, font);
        }

        BuffWrapper hovered = renderBoxes(graphics, buffs);
        if (hovered != null) {
            // Tags typed into the group name would be parsed by the tooltip, and malformed ones like <col=xyz>
            // throw while rendering it. The tooltip doesn't understand escapes like <lt>, so strip them instead.
            tooltipManager.add(new Tooltip(Text.removeTags(group.getName()) + ": " + hovered.getBuff().getName()));
        }

        // The full size even when boxes are hidden, so the overlay doesn't move when they come back
        return fullSize;
    }

    /**
     * Draws the shown boxes one after another and returns the hovered buff, if any.
     */
    private BuffWrapper renderBoxes(Graphics2D graphics, List<BuffWrapper> buffs) {
        // Component bounds are relative to the overlay, so move the mouse into the same space
        net.runelite.api.Point mouseCanvasPosition = client.getMouseCanvasPosition();
        Rectangle overlayBounds = getBounds();
        int mouseX = mouseCanvasPosition.getX() - overlayBounds.x;
        int mouseY = mouseCanvasPosition.getY() - overlayBounds.y;
        BuffWrapper hovered = null;

        Color presentColor = config.buffPresentColor();
        Color missingColor = getMissingColor();
        boolean horizontal = group.getOrientation() == ComponentOrientation.HORIZONTAL;
        int boxX = 0;
        int boxY = 0;
        int count = buffs.size();
        for (int i = 0; i < count; i++) {
            int index = group.isReversed() ? count - 1 - i : i;
            BuffWrapper buff = buffs.get(index);
            if (group.isMissingOnly() && buff.isPresent()) {
                continue;
            }

            BuffComponent component = components.get(index);
            component.setBackgroundColor(buff.isPresent() ? presentColor : missingColor);
            // Item images finish loading after the layout is built
            component.setImage(getImage(buff));
            component.setLocation(boxX, boxY);
            component.render(graphics);
            if (component.getBounds().contains(mouseX, mouseY)) {
                hovered = buff;
            }

            // Boxes touch, without a gap
            if (horizontal) {
                boxX += boxSize.width;
            } else {
                boxY += boxSize.height;
            }
        }
        return hovered;
    }

    private Dimension measureBoxSize(FontMetrics metrics) {
        boolean showText = group.isShowText();
        int textWidth = 0;
        if (showText) {
            for (BuffWrapper buff : group.getBuffs()) {
                textWidth = Math.max(textWidth, metrics.stringWidth(buff.getBuff().getName()));
            }
        }
        int textHeight = showText ? metrics.getHeight() : 0;
        int textRightPadding = showText ? PADDING : 0;

        boolean showIcon = group.isShowIcon();
        // Fixed square slot; scaled icons and the placeholder always fit in it
        int iconSize = showIcon ? config.buffIconSize() : 0;
        int iconPadding = showIcon ? PADDING : 0;

        int width = PADDING + iconSize + iconPadding + textWidth + textRightPadding;
        int height = PADDING + Math.max(textHeight, iconSize) + PADDING;
        return new Dimension(width, height);
    }

    private Dimension getFullSize(int count, Dimension boxSize) {
        return group.getOrientation() == ComponentOrientation.HORIZONTAL
                ? new Dimension(count * boxSize.width, boxSize.height)
                : new Dimension(boxSize.width, count * boxSize.height);
    }

    private Color getMissingColor() {
        int flashRate = 40;
        boolean isFlashing = group.isFlashMissing() && client.getGameCycle() % flashRate >= (flashRate / 2);
        // Alternates with RuneLite's "Overlay color", like any other overlay background
        return isFlashing ? runeLiteConfig.overlayBackgroundColor() : config.buffMissingColor();
    }

    private void createComponents(List<BuffWrapper> buffs, Font font) {
        components.clear();
        int iconSize = config.buffIconSize();
        for (BuffWrapper buff : buffs) {
            BuffComponent component = new BuffComponent();
            // Same font the box was measured with
            component.setFont(font);
            component.setPadding(PADDING);
            component.setIconSize(iconSize);
            component.setText(group.isShowText() ? buff.getBuff().getName() : null);
            component.setPreferredSize(boxSize);
            components.add(component);
        }
    }

    // Null when the group shows text only
    private BufferedImage getImage(BuffWrapper buff) {
        if (!group.isShowIcon()) {
            return null;
        }
        BufferedImage image = iconStore.getScaledImage(buff.getBuff());
        return image != null ? image : iconStore.getPlaceholderImage();
    }
}
