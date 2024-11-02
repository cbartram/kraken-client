package com.kraken;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KrakenLoaderPluginTest {
    public static void main(String[] args) throws Exception {
        System.out.println("External plugin manager loading?");
        ExternalPluginManager.loadBuiltin(KrakenLoaderPlugin.class);
        RuneLite.main(args);
    }
}
