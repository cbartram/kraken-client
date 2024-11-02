package com.kraken;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.externalplugins.PluginHubManifest;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Kraken Plugins",
        description = "Loads the Kraken Plugins.",
        tags = {"kraken", "plugin", "loader"}
)
public class KrakenLoaderPlugin extends Plugin {

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private Client client;

    private KrakenLoaderPanel panel;
    private NavigationButton navButton;

    private static final String PACKAGE_NAME = "com/krakenplugins";

    @Provides
    KrakenLoaderConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(KrakenLoaderConfig.class);
    }

    @Override
    protected void startUp() {
        panel = injector.getInstance(KrakenLoaderPanel.class);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "images/kraken.png");
        navButton = NavigationButton.builder()
                .tooltip("Kraken Plugins")
                .icon(icon)
                .priority(2)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        JarResourceLoader jarLoader = new JarResourceLoader();
        try {
            List<Class<Plugin>> pluginClasses = jarLoader.loadPluginClasses(PACKAGE_NAME);

            log.info("Loaded {} Kraken plugin class{}.", pluginClasses.size(), pluginClasses.size() > 1 ? "es" : "");
            for(Class<Plugin> clazz : pluginClasses) {
                ExternalPluginManager.loadBuiltin(clazz);
            }

//            ExternalPluginManager.loadBuiltin(pluginClasses.toArray(new Class[pluginClasses.size()]));
        } catch(MalformedURLException e) {
            log.error("URL Malformed. Error = {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void shutDown() {}
}