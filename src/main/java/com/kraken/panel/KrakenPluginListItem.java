package com.kraken.panel;

import com.kraken.KrakenLoaderPlugin;
import lombok.Getter;
import net.runelite.client.plugins.config.SearchablePlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KrakenPluginListItem extends JPanel implements SearchablePlugin {
    private static final ImageIcon ON_STAR;
	private static final ImageIcon OFF_STAR;
    private static final ImageIcon CONFIG_ICON;

	private final KrakenPluginListPanel pluginListPanel;

	@Getter
    private final PluginMetadata pluginConfig;

	@Getter
	private final List<String> keywords = new ArrayList<>();

	private final JToggleButton pinButton;
	private final PluginToggleButton onOffToggle;

	static {
        BufferedImage onStar = ImageUtil.loadImageResource(KrakenLoaderPlugin.class, "images/star_on.png");
        onStar = ImageUtil.recolorImage(onStar, new Color(106, 163, 60));
        ON_STAR = new ImageIcon(onStar);

		BufferedImage offStar = ImageUtil.luminanceScale(
			ImageUtil.grayscaleImage(onStar),
			0.77f
		);
		OFF_STAR = new ImageIcon(offStar);

        BufferedImage configIcon = ImageUtil.loadImageResource(KrakenLoaderPlugin.class, "images/config_edit_icon.png");
		CONFIG_ICON = new ImageIcon(configIcon);
	}

	KrakenPluginListItem(KrakenPluginListPanel pluginListPanel, PluginMetadata pluginConfig) {
		this.pluginListPanel = pluginListPanel;
		this.pluginConfig = pluginConfig;

		Collections.addAll(keywords, pluginConfig.getName().toLowerCase().split(" "));
		Collections.addAll(keywords, pluginConfig.getDescription().toLowerCase().split(" "));
		Collections.addAll(keywords, pluginConfig.getTags());

		setLayout(new BorderLayout(3, 0));
		setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 20));

		JLabel nameLabel = new JLabel(pluginConfig.getName());
		nameLabel.setForeground(Color.WHITE);

		if (!pluginConfig.getDescription().isEmpty())
		{
			nameLabel.setToolTipText("<html>" + pluginConfig.getName() + ":<br>" + pluginConfig.getDescription() + "</html>");
		}

		pinButton = new JToggleButton(OFF_STAR);
		pinButton.setSelectedIcon(ON_STAR);
		SwingUtil.removeButtonDecorations(pinButton);
		SwingUtil.addModalTooltip(pinButton, "Unpin plugin", "Pin plugin");
		pinButton.setPreferredSize(new Dimension(21, 0));
		add(pinButton, BorderLayout.LINE_START);

		pinButton.addActionListener(e -> {
			pluginListPanel.savePinnedPlugins();
			pluginListPanel.refresh();
		});

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		add(buttonPanel, BorderLayout.LINE_END);

		JMenuItem configMenuItem;
		if (pluginConfig.getConfigDescriptor() != null) {
			JButton configButton = new JButton(CONFIG_ICON);
			SwingUtil.removeButtonDecorations(configButton);
			configButton.setPreferredSize(new Dimension(25, 0));
			configButton.setVisible(false);
			buttonPanel.add(configButton);

			configButton.addActionListener(e -> {
				configButton.setIcon(CONFIG_ICON);
				openGroupConfigPanel();
			});

			configButton.setVisible(true);
			configButton.setToolTipText("Edit plugin configuration");

			configMenuItem = new JMenuItem("Configure");
			configMenuItem.addActionListener(e -> openGroupConfigPanel());
		}

		add(nameLabel, BorderLayout.CENTER);

		onOffToggle = new PluginToggleButton();
		onOffToggle.setConflicts(pluginConfig.getConflicts());
		buttonPanel.add(onOffToggle);
		if (pluginConfig.getPlugin() != null) {
			onOffToggle.addActionListener(i -> {
				if (onOffToggle.isSelected()) {
					pluginListPanel.startPlugin(pluginConfig.getPlugin());
				} else {
					pluginListPanel.stopPlugin(pluginConfig.getPlugin());
				}
			});
		} else {
			onOffToggle.setVisible(false);
		}
	}

	@Override
	public String getName() {
		return pluginConfig.getName();
	}

	@Override
	public String getSearchableName() {
		return pluginConfig.getName();
	}

	@Override
	public boolean isPinned() {
		return pinButton.isSelected();
	}

	void setPinned(boolean pinned) {
		pinButton.setSelected(pinned);
	}

	void setPluginEnabled(boolean enabled) {
		onOffToggle.setSelected(enabled);
	}

	private void openGroupConfigPanel() {
		pluginListPanel.openConfigurationPanel(pluginConfig);
	}

	/**
	 * Adds a mouseover effect to change the text of the passed label to {@link ColorScheme#BRAND_ORANGE} color, and
	 * adds the passed menu items to a popup menu shown when the label is clicked.
	 *
	 * @param label     The label to attach the mouseover and click effects to
	 * @param menuItems The menu items to be shown when the label is clicked
	 */
	static void addLabelPopupMenu(JLabel label, JMenuItem... menuItems)
	{
		final JPopupMenu menu = new JPopupMenu();
		final Color labelForeground = label.getForeground();
		menu.setBorder(new EmptyBorder(5, 5, 5, 5));

		for (final JMenuItem menuItem : menuItems)
		{
			if (menuItem == null)
			{
				continue;
			}

			// Some machines register mouseEntered through a popup menu, and do not register mouseExited when a popup
			// menu item is clicked, so reset the label's color when we click one of these options.
			menuItem.addActionListener(e -> label.setForeground(labelForeground));
			menu.add(menuItem);
		}

		label.addMouseListener(new MouseAdapter() {
			private Color lastForeground;

			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				Component source = (Component) mouseEvent.getSource();
				Point location = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(location, source);
				menu.show(source, location.x, location.y);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {
				lastForeground = label.getForeground();
				label.setForeground(ColorScheme.BRAND_ORANGE);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {
				label.setForeground(lastForeground);
			}
		});
	}
}
