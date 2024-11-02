package com.kraken;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("krakenloaderplugin")
public interface KrakenLoaderConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "General";
}