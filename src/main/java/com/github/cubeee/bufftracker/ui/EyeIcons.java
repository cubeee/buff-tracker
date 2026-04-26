package com.github.cubeee.bufftracker.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

final class EyeIcons {

    private static final int SIZE = 16;

    private static final Color VISIBLE_COLOR = Color.WHITE;
    private static final Color HIDDEN_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color HOVER_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

    static final ImageIcon VISIBLE = new ImageIcon(draw(VISIBLE_COLOR, false));
    static final ImageIcon VISIBLE_HOVER = new ImageIcon(draw(HOVER_COLOR, false));
    static final ImageIcon HIDDEN = new ImageIcon(draw(HIDDEN_COLOR, true));
    static final ImageIcon HIDDEN_HOVER = new ImageIcon(draw(HOVER_COLOR, true));

    private EyeIcons() {
    }

    private static BufferedImage draw(Color color, boolean crossedOut) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Almond-shaped outline
            Path2D outline = new Path2D.Double();
            outline.moveTo(1.5, 8);
            outline.quadTo(8, 1, 14.5, 8);
            outline.quadTo(8, 15, 1.5, 8);
            outline.closePath();
            graphics.draw(outline);

            // Pupil
            graphics.fill(new Ellipse2D.Double(5.5, 5.5, 5, 5));

            if (crossedOut) {
                graphics.draw(new Line2D.Double(2.5, 13.5, 13.5, 2.5));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
