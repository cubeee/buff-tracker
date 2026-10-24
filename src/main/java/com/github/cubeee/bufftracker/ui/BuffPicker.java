package com.github.cubeee.bufftracker.ui;

import com.github.cubeee.bufftracker.model.Buff;
import net.runelite.client.ui.ColorScheme;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
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

    // Set on the anchor when the picker closes and when the anchor is pressed. A press on the anchor closes the
    // picker too, and both happen at about the same time; that click shouldn't reopen it.
    private static final String CLOSED_AT_KEY = "buffPicker.closedAt";
    private static final String PRESSED_AT_KEY = "buffPicker.pressedAt";
    private static final long SAME_CLICK_WINDOW_MS = 250;

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
        KeyBindings.bind(list, JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER, "pick",
                () -> pick(list.getSelectedValue()));
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

        KeyBindings.bind(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW, KeyEvent.VK_ESCAPE, "close", this::dispose);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent event) {
                anchor.putClientProperty(CLOSED_AT_KEY, System.currentTimeMillis());
                dispose();
            }
        });
    }

    /**
     * Makes the anchor open the picker below it, or close it when it's already open.
     */
    static void attach(AbstractButton anchor, List<Buff> buffs, BuffCatalog catalog, Consumer<Buff> onPick) {
        anchor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                anchor.putClientProperty(PRESSED_AT_KEY, event.getWhen());
            }
        });
        anchor.addActionListener(e -> toggle(anchor, buffs, catalog, onPick));
    }

    private static void toggle(JComponent anchor, List<Buff> buffs, BuffCatalog catalog, Consumer<Buff> onPick) {
        Object closedAt = anchor.getClientProperty(CLOSED_AT_KEY);
        Object pressedAt = anchor.getClientProperty(PRESSED_AT_KEY);
        anchor.putClientProperty(CLOSED_AT_KEY, null);
        anchor.putClientProperty(PRESSED_AT_KEY, null);
        // The picker may close just before or just after the press is seen
        if (closedAt instanceof Long && pressedAt instanceof Long
                && Math.abs((Long) closedAt - (Long) pressedAt) < SAME_CLICK_WINDOW_MS) {
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
        // The list is the only focusable component, so it gets the focus when the picker opens
        picker.setVisible(true);
    }

    private void pick(Buff buff) {
        if (buff == null) {
            return;
        }
        dispose();
        onPick.accept(buff);
    }
}
