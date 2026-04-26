package com.github.cubeee.bufftracker;

import com.github.cubeee.bufftracker.config.ConfigKeys;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup(ConfigKeys.PLUGIN_CONFIG_GROUP_NAME)
public interface BuffTrackerConfig extends Config
{

	//
	// Sidepanel
	//

	@ConfigSection(
		name = "Panel",
		description = "",
		position = ConfigKeys.PANEL_SECTION_POSITION,
		closedByDefault = true
	)
	String PANEL_SECTION = "Panel";

	@ConfigSection(
		name = "Buffs",
		description = "",
		position = ConfigKeys.BUFFS_SECTION_POSITION
	)
	String BUFFS_SECTION = "Buffs";

	@ConfigSection(
		name = "Hotkeys",
		description = "",
		position = ConfigKeys.HOTKEYS_SECTION_POSITION
	)
	String HOTKEYS_SECTION = "Hotkeys";

	@ConfigItem(
		keyName = ConfigKeys.SHOW_PANEL,
		name = "Show sidepanel",
		description = "Disable if you don't want to see the sidepanel icon",
		section = PANEL_SECTION,
		position = 10
	)
	default boolean showPanel() {
		return true;
	}

	@ConfigItem(
		keyName = ConfigKeys.PANEL_PRIORITY,
		name = "Panel priority",
		description = "Sorting priority of the panel in the sidebar",
		section = PANEL_SECTION,
		position = 20
	)
	@Range(min = -10, max = 20)
	default int getPanelPriority() {
		return 10;
	}

	//
	// Hotkeys
	//

	@ConfigItem(
		keyName = ConfigKeys.CYCLE_GROUP_HOTKEY,
		name = "Cycle group",
		description = "Show the next group. Also works while typing in chat, so a key combination such as Ctrl+G is recommended",
		section = HOTKEYS_SECTION,
		position = 10
	)
	default Keybind cycleGroupHotkey() {
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = ConfigKeys.TOGGLE_VISIBILITY_HOTKEY,
		name = "Toggle visibility",
		description = "Hide the visible group, or show the last visible group again. Also works while typing in chat, so a key combination such as Ctrl+H is recommended",
		section = HOTKEYS_SECTION,
		position = 20
	)
	default Keybind toggleVisibilityHotkey() {
		return Keybind.NOT_SET;
	}

	//
	// Buffs
	//

	@ConfigItem(
		keyName = ConfigKeys.BUFF_ICON_SIZE,
		name = "Icon size",
		description = "",
		section = BUFFS_SECTION,
		position = 100
	)
	@Range(min = 2, max = 100)
	default int buffIconSize() {
		return 24;
	}

	@ConfigItem(
		keyName = ConfigKeys.BUFF_FONT,
		name = "Font",
		description = "",
		section = BUFFS_SECTION,
		position = 150
	)
	default FontType getFont() {
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = ConfigKeys.BUFF_PRESENT_COLOR,
		name = "Present color",
		description = "Background color of a present buff",
		section = BUFFS_SECTION,
		position = 300
	)
	@Alpha
	default Color buffPresentColor() {
		Color baseColor = Color.GREEN;
		return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 70);
	}

	@ConfigItem(
		keyName = ConfigKeys.BUFF_MISSING_COLOR,
		name = "Missing color",
		description = "Background color of a missing buff",
		section = BUFFS_SECTION,
		position = 400
	)
	@Alpha
	default Color buffMissingColor() {
		Color baseColor = Color.RED;
		return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 70);
	}

}
