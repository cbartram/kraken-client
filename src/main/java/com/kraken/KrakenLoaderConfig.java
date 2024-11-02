package com.kraken;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("krakenloaderplugin")
public interface KrakenLoaderConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "General";


    // General
    @ConfigItem(
            keyName = "hydraImmunityOutline",
            name = "Hydra immunity outline",
            description = "Overlay the hydra with a colored outline while it has immunity/not weakened.",
            position = 0,
            section = general
    )
    default boolean hydraImmunityOutline() {
        return false;
    }
}