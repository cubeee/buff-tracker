package com.github.cubeee.bufftracker.ui;

import com.github.cubeee.bufftracker.model.Buff;
import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.function.*;

final class BuffPicker extends JDialog {

    private static final Dimension SIZE = new Dimension(220, 260);

    // Set on the anchor when a click on it closed the picker, so the same click doesn't reopen it
    private static final String CLOSED_BY_ANCHOR_KEY = "buffPicker.closedByAnchorAt";
    private static final long CLOSED_BY_ANCHOR_WINDOW_MS = 1000;

    private final Consumer<Buff> onPick;

    private final DefaultListModel<Buff> model = new DefaultListModel<>();
    private final JList<Buff> list = new JList<>(model);

    private BuffPicker(Window owner, JComponent anchor, List<Buff> buffs, BuffCatalog catalog, Consumer<Buff> onPick) {
        super(owner);
        this.onPick = onPick;

        setUndecorated(true);
        setSize(SIZE);

        for (Buff buff : buffs) {
            model.addElement(buff);
        }

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Buff buff = (Buff) value;
                label.setText(buff.getName());
                label.setIcon(catalog.getIcon(buff));
                label.setIconTextGap(6);
                label.setBorder(new EmptyBorder(2, 4, 2, 4));
                return label;
            }
        });
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
        // Arrow keys are handled by JList itself
        list.registerKeyboardAction(e -> pick(list.getSelectedValue()),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    int index = list.locationToIndex(event.getPoint());
                    if (index >= 0 && list.getCellBounds(index, index).contains(event.getPoint())) {
                        pick(model.get(index));
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                new EmptyBorder(6, 6, 6, 6)));
        content.add(scrollPane, BorderLayout.CENTER);
        setContentPane(content);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent event) {
                if (isPointerOver(anchor)) {
                    anchor.putClientProperty(CLOSED_BY_ANCHOR_KEY, System.currentTimeMillis());
                }
                dispose();
            }
        });
    }

    static void toggle(JComponent anchor, List<Buff> buffs, BuffCatalog catalog, Consumer<Buff> onPick) {
        Object closedAt = anchor.getClientProperty(CLOSED_BY_ANCHOR_KEY);
        anchor.putClientProperty(CLOSED_BY_ANCHOR_KEY, null);
        if (closedAt instanceof Long && System.currentTimeMillis() - (Long) closedAt < CLOSED_BY_ANCHOR_WINDOW_MS) {
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(anchor);
        BuffPicker picker = new BuffPicker(owner, anchor, buffs, catalog, onPick);

        Point location = anchor.getLocationOnScreen();
        location.y += anchor.getHeight();

        // Keep the popup on the anchor's screen
        GraphicsConfiguration gc = anchor.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle screen = gc.getBounds();
            location.x = Math.max(screen.x, Math.min(location.x, screen.x + screen.width - SIZE.width));
            if (location.y + SIZE.height > screen.y + screen.height) {
                location.y = anchor.getLocationOnScreen().y - SIZE.height;
            }
        }

        picker.setLocation(location);
        picker.setVisible(true);
        picker.list.requestFocusInWindow();
    }

    private static boolean isPointerOver(Component component) {
        if (!component.isShowing()) {
            return false;
        }
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null) {
            return false;
        }
        Rectangle bounds = new Rectangle(component.getLocationOnScreen(), component.getSize());
        return bounds.contains(pointer.getLocation());
    }

    private void pick(Buff buff) {
        if (buff == null) {
            return;
        }
        dispose();
        onPick.accept(buff);
    }
}
