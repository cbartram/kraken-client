package com.kraken.panel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.plugins.Plugin;

import javax.annotation.Nullable;
import java.util.List;

@Value
@RequiredArgsConstructor
class PluginMetadata
{
	private final String name;
	private final String description;
	private final String[] tags;
	private final Plugin plugin;

	// Can be null if it has no more configuration than the on/off toggle
	@Nullable
	private final Config config;

	@Nullable
	private final ConfigDescriptor configDescriptor;

	@Nullable
	private final List<String> conflicts;

	PluginMetadata(String name, String description, String[] tags, Plugin plugin, Config config, ConfigDescriptor configDescriptor) {
		this(name, description, tags, plugin, config, configDescriptor, null);
	}
}