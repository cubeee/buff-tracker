package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.config.ConfigKeys;
import com.github.cubeee.bufftracker.model.Buff;
import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = "Buff Tracker",
	description = "Track groups of buffs such as potions and thralls, and see which ones are missing",
	tags = {"buff", "potion", "thrall", "timers", "divine", "antifire", "stamina"}
)
public class BuffTrackerPlugin extends Plugin {

	@Inject
	private ClientThread clientThread;

	@Inject
	private BuffIconStore iconStore;

	@Inject
	private GroupStore groupStore;

	@Inject
	private PanelController panelController;

	@Inject
	private BuffTrackerConfig config;

	@Inject
	private KeyManager keyManager;

	// Client thread only; set when a var changed, so the visible group's presence is refreshed once per cycle
	private boolean presenceDirty;

	private final HotkeyListener cycleGroupListener = new HotkeyListener(() -> config.cycleGroupHotkey()) {
		@Override
		public void hotkeyPressed() {
			clientThread.invokeLater(groupStore::cycleVisible);
		}
	};

	private final HotkeyListener toggleVisibilityListener = new HotkeyListener(() -> config.toggleVisibilityHotkey()) {
		@Override
		public void hotkeyPressed() {
			clientThread.invokeLater(groupStore::toggleVisibility);
		}
	};

	@Override
	protected void startUp() {
		panelController.startUp();

		// The stores are only used on the client thread. Queued after any pending shutDown() work, so a quick
		// restart can't have its listeners cleared by the previous run.
		clientThread.invokeLater(() -> {
			iconStore.setChangeListener(panelController::refreshIcons);
			groupStore.setChangeListener(panelController::refreshGroups);
			groupStore.setReloadListener(panelController::replaceGroups);
			groupStore.load();
			iconStore.startUp();
			panelController.refreshGroups();
		});

		keyManager.registerKeyListener(cycleGroupListener);
		keyManager.registerKeyListener(toggleVisibilityListener);
	}

	@Override
	protected void shutDown() {
		keyManager.unregisterKeyListener(cycleGroupListener);
		keyManager.unregisterKeyListener(toggleVisibilityListener);

		panelController.shutDown();
		clientThread.invokeLater(() -> {
			groupStore.clear();
			iconStore.shutDown();
		});
	}

	@Override
	public void resetConfiguration() {
		// Groups aren't config items, so the config panel's reset skips them
		clientThread.invokeLater(groupStore::reset);
	}

	@Provides
	@SuppressWarnings("unused")
	BuffTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(BuffTrackerConfig.class);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(ConfigChanged event) {
		if (!ConfigKeys.PLUGIN_CONFIG_GROUP_NAME.equals(event.getGroup())) {
			return;
		}

		// Groups are saved under this config group too. Most writes are the plugin's own, but they can also come
		// from elsewhere, e.g. config sync from another client.
		if (ConfigKeys.GROUPS.equals(event.getKey())) {
			clientThread.invoke(groupStore::reloadIfChanged);
			return;
		}

		if (ConfigKeys.BUFF_ICON_SIZE.equals(event.getKey())) {
			clientThread.invoke(() -> {
				iconStore.rescale();
				groupStore.invalidateLayouts();
			});
			return;
		}

		if (ConfigKeys.BUFF_FONT.equals(event.getKey())) {
			clientThread.invoke(groupStore::invalidateLayouts);
			return;
		}

		// The navigation button is only touched on the EDT; ConfigChanged can come from any thread
		String key = event.getKey();
		SwingUtilities.invokeLater(() -> panelController.onConfigChanged(key));
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onProfileChanged(ProfileChanged event) {
		// Groups and config values are stored per profile
		SwingUtilities.invokeLater(panelController::onProfileChanged);
		clientThread.invokeLater(() -> {
			iconStore.rescale();
			groupStore.reload();
		});
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onVarbitChanged(VarbitChanged event) {
		// Also posted for varp changes. Many can arrive in one cycle, so the refresh is batched in onClientTick.
		if (Buff.isTracked(event.getVarbitId(), event.getVarpId())) {
			presenceDirty = true;
		}
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		// Logging out or hopping may reset vars
		presenceDirty = true;
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onClientTick(ClientTick event) {
		if (presenceDirty) {
			presenceDirty = false;
			groupStore.refreshPresence(groupStore.getVisibleGroup());
		}
	}
}
