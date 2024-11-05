package com.kraken.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.kraken.KrakenLoaderPlugin;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;

@Singleton
public class TopLevelConfigPanel extends PluginPanel {
	private final MaterialTabGroup tabGroup;
	private final CardLayout layout;
	private final JPanel content;

	private final EventBus eventBus;
	private final KrakenLoaderPanel pluginListPanel;
	private final MaterialTab pluginListPanelTab;

	private boolean active = false;
	private PluginPanel current;
	private boolean removeOnTabChange;

	@Inject
	TopLevelConfigPanel(EventBus eventBus, KrakenLoaderPanel pluginListPanel) {
		super(false);

		this.eventBus = eventBus;

		tabGroup = new MaterialTabGroup();
		tabGroup.setLayout(new GridLayout(1, 0, 7, 7));
		tabGroup.setBorder(new EmptyBorder(10, 10, 0, 10));

		content = new JPanel();
		layout = new CardLayout();
		content.setLayout(layout);

		setLayout(new BorderLayout());
		add(tabGroup, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		this.pluginListPanel = pluginListPanel;

        MaterialTab mt = new MaterialTab(
        new ImageIcon(ImageUtil.loadImageResource(KrakenLoaderPlugin.class, "images/config_icon_lg.png")),
        tabGroup, null);
		mt.setToolTipText("Configuration");
		tabGroup.addTab(mt);

		content.add("config_icon_lg.png", pluginListPanel.getMuxer().getWrappedPanel());
		eventBus.register(pluginListPanel.getMuxer());

		mt.setOnSelectEvent(() ->
		{
			switchTo("config_icon_lg.png", pluginListPanel.getMuxer(), false);
			return true;
		});

		pluginListPanelTab = mt;
		tabGroup.select(pluginListPanelTab);
	}

	private void switchTo(String cardName, PluginPanel panel, boolean removeOnTabChange) {
		boolean doRemove = this.removeOnTabChange;
		PluginPanel prevPanel = current;
		if (active) {
			prevPanel.onDeactivate();
			panel.onActivate();
		}

		current = panel;
		this.removeOnTabChange = removeOnTabChange;

		layout.show(content, cardName);

		if (doRemove) {
			content.remove(prevPanel.getWrappedPanel());
			eventBus.unregister(prevPanel);
		}

		content.revalidate();
	}

	@Override
	public void onActivate()
	{
		active = true;
		current.onActivate();
	}

	@Override
	public void onDeactivate()
	{
		active = false;
		current.onDeactivate();
	}

	public void openConfigurationPanel(String name) {
		tabGroup.select(pluginListPanelTab);
		pluginListPanel.openConfigurationPanel(name);
	}

	public void openConfigurationPanel(Plugin plugin) {
		tabGroup.select(pluginListPanelTab);
		pluginListPanel.openConfigurationPanel(plugin);
	}

	public void openWithFilter(String filter) {
		tabGroup.select(pluginListPanelTab);
		pluginListPanel.openWithFilter(filter);
	}
}
