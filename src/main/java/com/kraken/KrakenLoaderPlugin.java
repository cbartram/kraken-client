package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.loader.JarResourceLoader;
import com.kraken.panel.KrakenLoaderPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.List;

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
    private PluginManager pluginManager;

    @Inject
	private Provider<KrakenLoaderPanel> pluginListPanelProvider;

    private NavigationButton navButton;

    private static final String PACKAGE_NAME = "com/krakenplugins";

    @Provides
    KrakenLoaderConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(KrakenLoaderConfig.class);
    }

    @Override
    protected void startUp() {
        KrakenLoaderPanel panel = pluginListPanelProvider.get();
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "images/kraken.png");
        navButton = NavigationButton.builder()
                .tooltip("Kraken Plugins")
                .icon(icon)
                .priority(2)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        loadKrakenPlugins();
        panel.rebuildPluginList();
    }

    @Override
    protected void shutDown() {}

    /**
     * Reads the downloaded JAR files, finds the Plugin classes, and invokes RuneLite's plugin manager
     * to side load the plugins.
     */
    private void loadKrakenPlugins() {
        JarResourceLoader jarLoader = new JarResourceLoader();
        try {
            List<Class<?>> pluginClasses = jarLoader.loadPluginClasses(PACKAGE_NAME);
            log.info("Loaded {} Kraken plugin class{}.", pluginClasses.size(), pluginClasses.size() > 1 ? "es" : "");
            List<Plugin> plugins = pluginManager.loadPlugins(pluginClasses, null);

            for(Plugin plugin : plugins) {
                pluginManager.setPluginEnabled(plugin, true);
                pluginManager.startPlugin(plugin);
            }
        } catch(MalformedURLException e) {
            log.error("URL Malformed. Error = {}", e.getMessage());
            e.printStackTrace();
        } catch(Exception e) {
            log.error("Exception thrown while attempting to invoke ExternalPluginManager refresh. Error = {}", e.getMessage());
            e.printStackTrace();
        }
    }
}