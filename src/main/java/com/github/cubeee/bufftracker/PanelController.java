package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.Buff;
import com.github.cubeee.bufftracker.model.BuffGroup;
import com.github.cubeee.bufftracker.config.ConfigKeys;
import com.github.cubeee.bufftracker.ui.BuffTrackerPanel;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.*;

@Singleton
class PanelController {

    private final ClientToolbar clientToolbar;
    private final ClientThread clientThread;
    private final BuffTrackerConfig config;
    private final BuffIconStore iconStore;
    private final GroupStore groupStore;
    private final GroupActions groupActions;

    private BuffTrackerPanel panel;
    private NavigationButton navigationButton;
    // Client thread only; icons finishing in the same client cycle share one panel rebuild
    private boolean iconRefreshPending;

    @Inject
    PanelController(
            ClientToolbar clientToolbar,
            ClientThread clientThread,
            BuffTrackerConfig config,
            BuffIconStore iconStore,
            GroupStore groupStore,
            GroupActions groupActions
    ) {
        this.clientToolbar = clientToolbar;
        this.clientThread = clientThread;
        this.config = config;
        this.iconStore = iconStore;
        this.groupStore = groupStore;
        this.groupActions = groupActions;
    }

    void startUp() {
        panel = new BuffTrackerPanel(groupActions);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panel_icon.png");

        navigationButton = NavigationButton.builder()
                .tooltip("Buff Tracker")
                .priority(config.getPanelPriority())
                .panel(panel)
                .icon(icon)
                .build();

        if (config.showPanel()) {
            clientToolbar.addNavigation(navigationButton);
        }
    }

    void shutDown() {
        clientToolbar.removeNavigation(navigationButton);
        navigationButton = null;
        panel = null;
    }

    void onConfigChanged(String key) {
        if (navigationButton == null) {
            return;
        }

        if (ConfigKeys.SHOW_PANEL.equals(key)) {
            if (config.showPanel()) {
                clientToolbar.addNavigation(navigationButton);
            } else {
                clientToolbar.removeNavigation(navigationButton);
            }
        }

        if (ConfigKeys.PANEL_PRIORITY.equals(key)) {
            recreateNavigationButton();
        }
    }

    // The new profile may hide the panel or give it another priority
    void onProfileChanged() {
        if (navigationButton != null) {
            recreateNavigationButton();
        }
    }

    // Called on the client thread; the panel is only touched on the EDT, where the plugin starts and stops
    void refreshIcons() {
        if (iconRefreshPending) {
            return;
        }
        iconRefreshPending = true;
        clientThread.invokeLater(() -> {
            iconRefreshPending = false;
            Map<Buff, BufferedImage> images = iconStore.snapshot();
            SwingUtilities.invokeLater(() -> {
                if (panel != null) {
                    panel.setIcons(images);
                }
            });
        });
    }

    void refreshGroups() {
        List<BuffGroup> groups = groupStore.snapshot();
        SwingUtilities.invokeLater(() -> {
            if (panel != null) {
                panel.setGroups(groups);
            }
        });
    }

    // The groups were replaced, e.g. by another profile's
    void replaceGroups() {
        List<BuffGroup> groups = groupStore.snapshot();
        SwingUtilities.invokeLater(() -> {
            if (panel != null) {
                panel.replaceGroups(groups);
            }
        });
    }

    private void recreateNavigationButton() {
        clientToolbar.removeNavigation(navigationButton);
        navigationButton = NavigationButton.builder()
                .tooltip(navigationButton.getTooltip())
                .priority(config.getPanelPriority())
                .panel(navigationButton.getPanel())
                .icon(navigationButton.getIcon())
                .build();
        if (config.showPanel()) {
            clientToolbar.addNavigation(navigationButton);
        }
    }
}
