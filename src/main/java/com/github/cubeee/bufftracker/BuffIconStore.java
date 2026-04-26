package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.Buff;
import com.github.cubeee.bufftracker.model.BuffIcon;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Item and sprite icons for every buff. Used on the client thread only.
 */
@Singleton
class BuffIconStore {

    private final ItemManager itemManager;
    private final SpriteManager spriteManager;
    private final BuffTrackerConfig config;

    // Item images are filled in asynchronously; only loaded ones are handed out
    private final Map<Buff, BufferedImage> images = new EnumMap<>(Buff.class);
    private final Map<Buff, BufferedImage> scaledImages = new EnumMap<>(Buff.class);
    private BufferedImage placeholderImage;
    private boolean started;
    private Runnable changeListener = () -> {};

    @Inject
    BuffIconStore(ItemManager itemManager, SpriteManager spriteManager, BuffTrackerConfig config) {
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;
        this.config = config;
    }

    void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    void startUp() {
        started = true;
        for (Buff buff : Buff.values()) {
            BuffIcon icon = buff.getIcon();
            if (icon.getSource() == BuffIcon.Source.SPRITE) {
                // Retried on the client thread until the sprite is available
                spriteManager.getSpriteAsync(icon.getId(), 0, image -> onImageLoaded(buff, image));
            } else {
                AsyncBufferedImage image = itemManager.getImage(icon.getId());
                image.onLoaded(() -> onImageLoaded(buff, image));
            }
        }
    }

    void shutDown() {
        started = false;
        images.clear();
        scaledImages.clear();
        placeholderImage = null;
        changeListener = () -> {};
    }

    private void onImageLoaded(Buff buff, BufferedImage image) {
        if (!started) {
            // Shut down while loading
            return;
        }
        images.put(buff, image);
        scaledImages.remove(buff);
        changeListener.run();
    }

    /**
     * Returns null while the item image is still loading.
     */
    BufferedImage getScaledImage(Buff buff) {
        BufferedImage scaled = scaledImages.get(buff);
        if (scaled == null) {
            BufferedImage image = images.get(buff);
            if (image == null) {
                return null;
            }
            scaled = scaleImage(image);
            scaledImages.put(buff, scaled);
        }
        return scaled;
    }

    void rescale() {
        scaledImages.clear();
        placeholderImage = null;
    }

    Map<Buff, BufferedImage> snapshot() {
        return new EnumMap<>(images);
    }

    // Shown while an item image is loading, so the box always has something to show
    BufferedImage getPlaceholderImage() {
        if (placeholderImage == null) {
            placeholderImage = createPlaceholderImage(config.buffIconSize());
        }
        return placeholderImage;
    }

    private static BufferedImage createPlaceholderImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);

            float stroke = Math.max(1f, size / 16f);
            graphics.setStroke(new BasicStroke(stroke));
            float inset = stroke / 2;
            float arc = size / 4f;
            graphics.draw(new RoundRectangle2D.Float(inset, inset, size - stroke, size - stroke, arc, arc));

            graphics.setFont(new Font(Font.DIALOG, Font.BOLD, Math.max(8, Math.round(size * 0.65f))));
            FontMetrics metrics = graphics.getFontMetrics();
            int textX = (size - metrics.stringWidth("?")) / 2;
            int textY = (size - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString("?", textX, textY);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    // Fits the image inside the icon size, keeping its aspect ratio
    private BufferedImage scaleImage(BufferedImage image) {
        int targetSize = config.buffIconSize();
        return ImageUtil.resizeImage(image, targetSize, targetSize, true);
    }
}
