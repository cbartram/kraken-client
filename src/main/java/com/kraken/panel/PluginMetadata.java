package com.kraken.panel;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.plugins.Plugin;

import javax.annotation.Nullable;
import java.util.List;

@Value
@RequiredArgsConstructor
public class PluginMetadata {
	String name;
	String description;
	String[] tags;
	Plugin plugin;
	Boolean verified;

	// Can be null if it has no more configuration than the on/off toggle
	@Nullable
	Config config;

	@Nullable
	ConfigDescriptor configDescriptor;

	@Nullable
	List<String> conflicts;

	PluginMetadata(String name, String description, String[] tags, Plugin plugin, boolean verified, Config config, ConfigDescriptor configDescriptor) {
		this(name, description, tags, plugin, verified, config, configDescriptor, null);
	}
}