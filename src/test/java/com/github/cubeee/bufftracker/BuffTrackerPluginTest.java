package com.github.cubeee.bufftracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// Launches the development client with the plugin; not a unit test
@SuppressWarnings("PMD")
public class BuffTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BuffTrackerPlugin.class);
		RuneLite.main(args);
	}
}
