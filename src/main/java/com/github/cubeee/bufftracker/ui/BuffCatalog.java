package com.github.cubeee.bufftracker.ui;

import com.github.cubeee.bufftracker.model.Buff;
import net.runelite.client.util.ImageUtil;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.util.*;

class BuffCatalog {

    static final int ICON_SIZE = 24;

    // BufferedImage uses identity equality, so replaced images drop their icons once unreferenced
    private final Map<BufferedImage, ImageIcon> scaledIcons = new WeakHashMap<>();

    private Map<Buff, BufferedImage> images = Collections.emptyMap();

    void setImages(Map<Buff, BufferedImage> images) {
        this.images = images;
    }

    static Comparator<Buff> byName() {
        return Comparator.comparing(Buff::getName, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Returns null while the buff's item image is still loading.
     */
    ImageIcon getIcon(Buff buff) {
        BufferedImage image = images.get(buff);
        if (image == null) {
            return null;
        }
        return scaledIcons.computeIfAbsent(image,
                img -> new ImageIcon(ImageUtil.resizeImage(img, ICON_SIZE, ICON_SIZE, true)));
    }
}
