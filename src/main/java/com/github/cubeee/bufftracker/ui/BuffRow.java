package com.github.cubeee.bufftracker.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

class BuffRow extends JPanel {

    BuffRow(String text, ImageIcon icon, JComponent trailing) {
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(BuffCatalog.ICON_SIZE, BuffCatalog.ICON_SIZE));
        if (icon != null) {
            iconLabel.setIcon(icon);
        } else {
            iconLabel.setText("?");
            iconLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        }

        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);

        add(iconLabel, BorderLayout.WEST);
        add(label, BorderLayout.CENTER);
        if (trailing != null) {
            trailing.setOpaque(false);
            add(trailing, BorderLayout.EAST);
        }
    }
}
