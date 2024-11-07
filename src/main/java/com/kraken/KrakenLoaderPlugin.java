package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.panel.KrakenPluginListPanel;
import com.kraken.panel.RootPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Kraken Plugins",
        description = "Loads the Kraken Plugins.",
        hidden = true,
        tags = {"kraken", "plugin", "loader"}
)
public class KrakenLoaderPlugin extends Plugin {

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private PluginManager pluginManager;

    @Inject
	private Provider<KrakenPluginListPanel> pluginListPanelProvider;

    @Inject
    private Provider<RootPanel> rootPanelProvider;

    @Inject
    private KrakenPluginManager krakenPluginManager;

    private NavigationButton navButton;

    @Override
    protected void startUp() {
        RootPanel panelRoot = rootPanelProvider.get();
        KrakenPluginListPanel panel = pluginListPanelProvider.get();
        krakenPluginManager.loadKrakenPlugins();
        krakenPluginManager.getPluginMap().put("Kraken Plugins", this);
        panel.rebuildPluginList();

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "images/kraken.png");
        navButton = NavigationButton.builder()
                .tooltip("Kraken Plugins")
                .icon(icon)
                .priority(2)
                .panel(panelRoot)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
    }

}