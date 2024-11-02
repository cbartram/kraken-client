package com.kraken;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KrakenLoaderPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(KrakenLoaderPlugin.class);
        RuneLite.main(args);
    }
}
