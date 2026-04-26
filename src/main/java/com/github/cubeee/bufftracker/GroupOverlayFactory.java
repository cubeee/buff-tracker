package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.model.BuffGroup;
import net.runelite.api.Client;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds what every group overlay needs, so the store only deals with groups.
 */
@Singleton
class GroupOverlayFactory {

    private final BuffTrackerPlugin plugin;
    private final BuffIconStore iconStore;
    private final BuffTrackerConfig config;
    private final RuneLiteConfig runeLiteConfig;
    private final TooltipManager tooltipManager;
    private final Client client;

    @Inject
    GroupOverlayFactory(
            BuffTrackerPlugin plugin,
            BuffIconStore iconStore,
            BuffTrackerConfig config,
            RuneLiteConfig runeLiteConfig,
            TooltipManager tooltipManager,
            Client client
    ) {
        this.plugin = plugin;
        this.iconStore = iconStore;
        this.config = config;
        this.runeLiteConfig = runeLiteConfig;
        this.tooltipManager = tooltipManager;
        this.client = client;
    }

    GroupOverlay create(BuffGroup group) {
        return new GroupOverlay(plugin, group, iconStore, config, runeLiteConfig, tooltipManager, client);
    }
}
