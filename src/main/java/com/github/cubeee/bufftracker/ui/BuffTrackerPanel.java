package com.github.cubeee.bufftracker.ui;

import com.github.cubeee.bufftracker.GroupActions;
import com.github.cubeee.bufftracker.model.Buff;
import com.github.cubeee.bufftracker.model.BuffGroup;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

public class BuffTrackerPanel extends PluginPanel {

    private final GroupActions actions;
    private final BuffCatalog catalog = new BuffCatalog();

    private final JPanel groupsPanel = new JPanel();

    private final Set<String> expandedGroupIds = new HashSet<>();
    private List<BuffGroup> groups = Collections.emptyList();
    private Set<String> knownGroupIds;
    // The cards currently shown, by group id
    private final Map<String, GroupCard> cards = new HashMap<>();

    // A group name is being edited. Rebuilding would remove the field and drop the edit, so wait for it to end.
    private boolean editing;
    private boolean rebuildPending;

    public BuffTrackerPanel(GroupActions groupActions) {
        super(false);
        this.actions = groupActions;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(getHeader(), BorderLayout.NORTH);
        add(createGroupsList(), BorderLayout.CENTER);

        rebuildGroups();
    }

    @Nonnull
    private JPanel getHeader() {
        JLabel title = new JLabel("Buff Tracker");
        title.setForeground(Color.WHITE);

        JButton addGroupButton = new JButton("+");
        addGroupButton.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        addGroupButton.setToolTipText("Add group");
        addGroupButton.setFocusPainted(false);
        addGroupButton.addActionListener(e -> actions.createGroup());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        header.add(title, BorderLayout.WEST);
        header.add(addGroupButton, BorderLayout.EAST);
        return header;
    }

    private JComponent createGroupsList() {
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(new ViewportWidthPanel(groupsPanel));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        return scrollPane;
    }

    public void setGroups(List<BuffGroup> groups) {
        trackExpandedGroups(groups);

        List<BuffGroup> oldGroups = this.groups;
        this.groups = groups;
        if (rebuildPending || !haveSameIds(oldGroups, groups)) {
            rebuildGroups();
        } else {
            updateCards(oldGroups, groups);
        }
    }

    private void trackExpandedGroups(List<BuffGroup> groups) {
        Set<String> ids = new HashSet<>();
        for (BuffGroup group : groups) {
            ids.add(group.getId());
            // New groups start expanded
            if (knownGroupIds != null && !knownGroupIds.contains(group.getId())) {
                expandedGroupIds.add(group.getId());
            }
        }
        expandedGroupIds.retainAll(ids);
        knownGroupIds = ids;
    }

    /**
     * Updates the cards for the same groups in the same order. Usually a single group was edited. Toggling
     * visibility, e.g. with the hotkeys, only needs the eye buttons updated; other changes replace just the changed
     * cards.
     */
    private void updateCards(List<BuffGroup> oldGroups, List<BuffGroup> groups) {
        List<BuffGroup> changed = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            BuffGroup group = groups.get(i);
            if (oldGroups.get(i).equalsIgnoringVisibility(group)) {
                cards.get(group.getId()).setGroupVisible(group.isVisible());
            } else {
                changed.add(group);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        if (editing) {
            // The edited name field may be in one of the cards
            rebuildGroups();
            return;
        }
        for (BuffGroup group : changed) {
            replaceCard(group);
        }
        groupsPanel.revalidate();
        groupsPanel.repaint();
    }

    /**
     * Shows a different set of groups, e.g. another profile's, all collapsed.
     */
    public void replaceGroups(List<BuffGroup> groups) {
        knownGroupIds = null;
        expandedGroupIds.clear();
        // Expanded cards must be rebuilt collapsed, even if the new groups only differ in visibility
        rebuildPending = true;
        setGroups(groups);
    }

    // Same groups in the same order, so each card can be updated in place
    private static boolean haveSameIds(List<BuffGroup> oldGroups, List<BuffGroup> newGroups) {
        if (oldGroups.size() != newGroups.size()) {
            return false;
        }
        for (int i = 0; i < oldGroups.size(); i++) {
            if (!oldGroups.get(i).getId().equals(newGroups.get(i).getId())) {
                return false;
            }
        }
        return true;
    }

    public void setIcons(Map<Buff, BufferedImage> images) {
        catalog.setImages(images);
        if (!canReplaceCards()) {
            rebuildGroups();
            return;
        }
        // Only expanded cards show buff icons
        for (BuffGroup group : groups) {
            if (expandedGroupIds.contains(group.getId())) {
                replaceCard(group);
            }
        }
        groupsPanel.revalidate();
        groupsPanel.repaint();
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        if (!editing && rebuildPending) {
            // Called from the field's focus listener; let the focus change finish before the field is removed
            SwingUtilities.invokeLater(this::rebuildGroups);
        }
    }

    private void rebuildGroups() {
        if (editing) {
            rebuildPending = true;
            return;
        }
        rebuildPending = false;

        groupsPanel.removeAll();
        cards.clear();

        if (groups.isEmpty()) {
            JLabel emptyLabel = new JLabel("No groups, click + to add one");
            emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            groupsPanel.add(emptyLabel);
        }

        for (BuffGroup group : groups) {
            GroupCard card = createCard(group);
            cards.put(group.getId(), card);
            groupsPanel.add(card);
            groupsPanel.add(Box.createVerticalStrut(4));
        }

        groupsPanel.revalidate();
        groupsPanel.repaint();
    }

    private GroupCard createCard(BuffGroup group) {
        String id = group.getId();
        Runnable toggle = () -> {
            if (!expandedGroupIds.remove(id)) {
                expandedGroupIds.add(id);
            }
            if (!canReplaceCards()) {
                rebuildGroups();
                return;
            }
            // The card's own group may be outdated, e.g. after a visibility-only update
            for (BuffGroup current : groups) {
                if (current.getId().equals(id)) {
                    replaceCard(current);
                }
            }
            groupsPanel.revalidate();
            groupsPanel.repaint();
        };
        return new GroupCard(group, expandedGroupIds.contains(id), toggle, this::setEditing, actions, catalog);
    }

    // The cards match the groups and no name field would be removed mid-edit
    private boolean canReplaceCards() {
        return !editing && !rebuildPending;
    }

    /**
     * Swaps a card for a new one built from the group; the caller revalidates the panel.
     */
    private void replaceCard(BuffGroup group) {
        GroupCard oldCard = cards.get(group.getId());
        int index = groupsPanel.getComponentZOrder(oldCard);
        GroupCard card = createCard(group);
        groupsPanel.remove(index);
        groupsPanel.add(card, index);
        cards.put(group.getId(), card);
    }
}
