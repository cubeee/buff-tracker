package com.github.cubeee.bufftracker.ui;

import com.github.cubeee.bufftracker.GroupActions;
import com.github.cubeee.bufftracker.model.Buff;
import com.github.cubeee.bufftracker.model.BuffGroup;
import com.github.cubeee.bufftracker.model.BuffWrapper;
import com.github.cubeee.bufftracker.config.DisplayMode;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

import javax.annotation.Nonnull;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.*;
import java.util.function.*;

class GroupCard extends JPanel {

    // RuneLite's default font lacks arrow glyphs
    private static final Font SYMBOL_FONT = new Font(Font.DIALOG, Font.PLAIN, 11);

    private enum Direction {
        RIGHT("→", "Horizontal", ComponentOrientation.HORIZONTAL, false),
        LEFT("←", "Horizontal, reversed", ComponentOrientation.HORIZONTAL, true),
        DOWN("↓", "Vertical", ComponentOrientation.VERTICAL, false),
        UP("↑", "Vertical, reversed", ComponentOrientation.VERTICAL, true);

        private final String symbol;
        private final String tooltip;
        private final ComponentOrientation orientation;
        private final boolean reversed;

        Direction(String symbol, String tooltip, ComponentOrientation orientation, boolean reversed) {
            this.symbol = symbol;
            this.tooltip = tooltip;
            this.orientation = orientation;
            this.reversed = reversed;
        }
    }

    private final BuffGroup group;
    private final GroupActions actions;
    private final BuffCatalog catalog;

    private JToggleButton visibleButton;
    private InlineTextField nameField;

    GroupCard(
            BuffGroup group,
            boolean expanded,
            Runnable onToggleExpanded,
            Consumer<Boolean> onEditingChanged,
            GroupActions actions,
            BuffCatalog catalog
    ) {
        this.group = group;
        this.actions = actions;
        this.catalog = catalog;

        // No vgap: BorderLayout adds it below NORTH even without a CENTER, unbalancing collapsed cards
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        add(createHeader(expanded, onToggleExpanded, onEditingChanged), BorderLayout.NORTH);
        setGroupVisible(group.isVisible());
        if (expanded) {
            add(createBody(), BorderLayout.CENTER);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        // Stretch horizontally in the BoxLayout, but never vertically
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
     * Updates the card in place when only the group's visibility changed.
     */
    final void setGroupVisible(boolean visible) {
        visibleButton.setSelected(visible);
        visibleButton.setToolTipText(visible ? "Hide overlay" : "Show overlay");
        nameField.setForeground(visible ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
    }

    private JPanel createHeader(boolean expanded, Runnable onToggleExpanded, Consumer<Boolean> onEditingChanged) {
        JButton expandButton = symbolButton(expanded ? "▼" : "▶", expanded ? "Collapse" : "Expand");
        expandButton.addActionListener(e -> onToggleExpanded.run());

        visibleButton = createVisibleButton();

        JButton menuButton = symbolButton("…", "More");
        menuButton.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(ev -> confirmDelete());
            menu.add(delete);
            menu.show(menuButton, 0, menuButton.getHeight());
        });

        nameField = new InlineTextField(group.getName(), name -> {
            if (name.isEmpty()) {
                return false;
            }
            actions.setName(group.getId(), name);
            return true;
        }, BuffGroup.MAX_NAME_LENGTH, onEditingChanged);
        nameField.setToolTipText("Click to rename");

        // GridBag centers every component vertically within the row
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 0, 2);
        header.add(expandButton, constraints);
        header.add(visibleButton, constraints);
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        header.add(nameField, constraints);
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        header.add(menuButton, constraints);
        return header;
    }

    @Nonnull
    private JToggleButton createVisibleButton() {
        JToggleButton visibleButton = new JToggleButton(EyeIcons.HIDDEN);
        visibleButton.setSelectedIcon(EyeIcons.VISIBLE);
        visibleButton.setRolloverIcon(EyeIcons.HIDDEN_HOVER);
        visibleButton.setRolloverSelectedIcon(EyeIcons.VISIBLE_HOVER);
        visibleButton.setBorder(new EmptyBorder(0, 2, 0, 2));
        visibleButton.setContentAreaFilled(false);
        visibleButton.setFocusPainted(false);
        visibleButton.addActionListener(e -> actions.setVisible(group.getId(), visibleButton.isSelected()));
        return visibleButton;
    }

    private JPanel createBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(6, 0, 0, 0));

        body.add(createSettings());

        List<BuffWrapper> members = group.getBuffs();
        JPanel membersPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        membersPanel.setOpaque(false);
        membersPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        for (int i = 0; i < members.size(); i++) {
            membersPanel.add(createMemberRow(members.get(i).getBuff(), i == 0, i == members.size() - 1));
        }
        membersPanel.add(createAddButton());

        body.add(membersPanel);
        return body;
    }

    private JPanel createSettings() {
        JComboBox<DisplayMode> displayBox = new JComboBox<>(DisplayMode.values());
        displayBox.setSelectedItem(group.getDisplayMode());
        displayBox.addActionListener(e -> {
            // Also fired when the current mode is picked again
            DisplayMode mode = (DisplayMode) displayBox.getSelectedItem();
            if (mode != group.getDisplayMode()) {
                actions.setDisplayMode(group.getId(), mode);
            }
        });

        // Labels take their natural width, controls get the rest
        JPanel settings = new JPanel(new GridBagLayout());
        settings.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 4, 6);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridy = 0;
        settings.add(label("Display"), constraints);
        constraints.gridy = 1;
        settings.add(label("Direction"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 4, 0);
        constraints.gridy = 0;
        settings.add(displayBox, constraints);
        constraints.gridy = 1;
        settings.add(createDirectionButtons(), constraints);

        // Checkboxes span both columns
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 2;
        settings.add(checkBox("Show missing only", "Only show the buffs that are missing",
                group.isMissingOnly(), selected -> actions.setMissingOnly(group.getId(), selected)), constraints);
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 0, 0);
        settings.add(checkBox("Flash missing buffs", "Alternate missing buffs between the missing color and RuneLite's overlay color",
                group.isFlashMissing(), selected -> actions.setFlashMissing(group.getId(), selected)), constraints);
        return settings;
    }

    private static JCheckBox checkBox(String text, String tooltip, boolean selected, Consumer<Boolean> onChange) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setToolTipText(tooltip);
        checkBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.addActionListener(e -> onChange.accept(checkBox.isSelected()));
        return checkBox;
    }

    private JPanel createDirectionButtons() {
        JPanel panel = new JPanel(new GridLayout(1, 0, 2, 0));
        panel.setOpaque(false);

        ButtonGroup buttonGroup = new ButtonGroup();
        for (Direction direction : Direction.values()) {
            JToggleButton button = new JToggleButton(direction.symbol);
            styleSymbolButton(button, direction.tooltip);
            boolean current = group.getOrientation() == direction.orientation && group.isReversed() == direction.reversed;
            button.setSelected(current);
            // Clicking the selected button keeps it selected; nothing changes
            if (!current) {
                button.addActionListener(e -> actions.setDirection(group.getId(), direction.orientation, direction.reversed));
            }
            buttonGroup.add(button);
            panel.add(button);
        }
        return panel;
    }

    private JButton createAddButton() {
        List<Buff> candidates = new ArrayList<>();
        for (Buff buff : Buff.values()) {
            if (!group.contains(buff)) {
                candidates.add(buff);
            }
        }
        candidates.sort(BuffCatalog.byName());

        JButton addButton = new JButton("+ Add buff");
        addButton.setFocusPainted(false);
        if (candidates.isEmpty()) {
            addButton.setEnabled(false);
            addButton.setToolTipText("All buffs are already in this group");
        }
        addButton.addActionListener(e -> BuffPicker.toggle(addButton, candidates, catalog,
                buff -> actions.addBuff(group.getId(), buff)));
        return addButton;
    }

    private JComponent createMemberRow(Buff buff, boolean first, boolean last) {
        JButton up = symbolButton("▲", "Move up");
        up.setEnabled(!first);
        up.addActionListener(e -> actions.moveBuff(group.getId(), buff, -1));

        JButton down = symbolButton("▼", "Move down");
        down.setEnabled(!last);
        down.addActionListener(e -> actions.moveBuff(group.getId(), buff, 1));

        JButton remove = symbolButton("✕", "Remove from group");
        remove.addActionListener(e -> actions.removeBuff(group.getId(), buff));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        buttons.add(up);
        buttons.add(down);
        buttons.add(remove);

        BuffRow row = new BuffRow(buff.getName(), catalog.getIcon(buff), buttons);
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        return row;
    }

    private void confirmDelete() {
        int result = JOptionPane.showConfirmDialog(this,
                "Delete group \"" + group.getName() + "\"?",
                "Delete group",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            actions.deleteGroup(group.getId());
        }
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        return label;
    }

    private static JButton symbolButton(String text, String tooltip) {
        JButton button = new JButton(text);
        styleSymbolButton(button, tooltip);
        return button;
    }

    private static void styleSymbolButton(AbstractButton button, String tooltip) {
        button.setFont(SYMBOL_FONT);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(0, 2, 0, 2));
        button.setFocusPainted(false);
    }
}
