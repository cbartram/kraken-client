package com.kraken.panel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Singleton
public class RootPanel extends PluginPanel {
	private final MaterialTabGroup tabGroup;
	private final CardLayout layout;
	private final JPanel content;

	@Getter
	private final KrakenPluginListPanel pluginListPanel;

	@Inject
	RootPanel(EventBus eventBus, KrakenPluginListPanel pluginListPanel) {
		super(false);

		this.pluginListPanel = pluginListPanel;

		tabGroup = new MaterialTabGroup();
		tabGroup.setLayout(new GridLayout(1, 0, 7, 7));
		tabGroup.setBorder(new EmptyBorder(10, 10, 0, 10));

		content = new JPanel();
		layout = new CardLayout();
		content.setLayout(layout);

		setLayout(new BorderLayout());
		add(tabGroup, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		content.add("images/config_icon_lg.png", pluginListPanel.getMuxer().getWrappedPanel());
		eventBus.register(pluginListPanel.getMuxer());
	}
}
