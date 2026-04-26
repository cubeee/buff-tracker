package com.github.cubeee.bufftracker.overlay;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

@Setter
public class BuffComponent implements RenderableEntity {

    private Font font;
    private int padding;
    private String text;
    private Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
    private BufferedImage image;
    // Width of the icon slot; images are centered in it so text lines up across boxes
    private int iconSize;

    private Dimension preferredSize;

    // Position within the overlay, and the area last drawn there
    @Getter
    private final Rectangle bounds = new Rectangle();
    private int locationX;
    private int locationY;

    // Rendered every frame, so reused instead of recreated
    private final BackgroundComponent backgroundComponent = new BackgroundComponent();
    private final TextComponent textComponent = new TextComponent();

    public void setLocation(int locationX, int locationY) {
        this.locationX = locationX;
        this.locationY = locationY;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        final int baseX = locationX;
        final int baseY = locationY;

        // Calculate dimensions
        bounds.setBounds(baseX, baseY, preferredSize.width, preferredSize.height);

        // Render background
        backgroundComponent.setBackgroundColor(backgroundColor);
        backgroundComponent.setRectangle(bounds);
        backgroundComponent.render(graphics);

        if (image != null) {
            int slotWidth = Math.max(iconSize, image.getWidth());
            int imageX = baseX + padding + (slotWidth - image.getWidth()) / 2;
            int imageY = baseY + (bounds.height - image.getHeight()) / 2;
            graphics.drawImage(image, imageX, imageY, null);
        }

        int offsetX = image != null ? Math.max(iconSize, image.getWidth()) + padding * 2 : padding;

        // Render caption
        if (!Strings.isNullOrEmpty(text))
        {
            graphics.setFont(font);
            FontMetrics fontMetrics = graphics.getFontMetrics();
            // Baseline that centres the text's full height vertically
            int textY = baseY + (bounds.height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

            // TextComponent's default white, unoutlined text
            textComponent.setText(text);
            textComponent.setPosition(baseX + offsetX, textY);
            textComponent.render(graphics);
        }

        return preferredSize;
    }
}
