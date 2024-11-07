package com.kraken.panel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.KrakenLoaderPlugin;
import com.kraken.KrakenPluginManager;
import com.kraken.api.*;
import com.kraken.auth.DiscordAuth;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.*;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.config.PluginSearch;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.MultiplexingPluginPanel;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class KrakenPluginListPanel extends PluginPanel {

	private static final String RUNELITE_GROUP_NAME = RuneLiteConfig.class.getAnnotation(ConfigGroup.class).value();
	private static final String PINNED_PLUGINS_CONFIG_KEY = "krakenPinnedPlugins";
	private static final String DISCONNECT_DISCORD_BUTTON_TEXT = "Disconnect Discord";
	private static final String SIGN_IN_DISCORD_BUTTON_TEXT = "Sign-in with Discord";

    private List<KrakenPluginListItem> pluginList;
    private final JPanel display = new JPanel();
    private final MaterialTabGroup tabGroup = new MaterialTabGroup(display);
    private final IconTextField searchBar;
    private final JScrollPane scrollPane;
	private final JButton discordButton;
	private static ImageIcon discordIcon;

	@Getter
    private final FixedWidthPanel mainPanel;

    private final ConfigManager configManager;
    private final List<PluginMetadata> fakePlugins = new ArrayList<>();
    private final Provider<ConfigPanel> configPanelProvider;
	private final PluginManager pluginManager;
	private final KrakenPluginManager krakenPluginManager;
	private final KrakenClient krakenClient;
	private final DiscordAuth discordAuth;
	private final KrakenCredentialManager credentialManager;

    @Getter
	private final MultiplexingPluginPanel muxer;

	static {
		BufferedImage icon = ImageUtil.loadImageResource(KrakenLoaderPlugin.class, "images/discord_icon.png");
		icon = ImageUtil.resizeImage(icon, 18, 16);
		discordIcon = new ImageIcon(icon);
	}

    @Inject
    public KrakenPluginListPanel(EventBus eventBus,
								 PluginManager pluginManager,
								 KrakenPluginManager krakenPluginManager,
								 ConfigManager configManager,
								 KrakenClient krakenClient,
								 KrakenCredentialManager credentialManager,
								 DiscordAuth discordAuth,
								 Provider<ConfigPanel> configPanelProvider) {
        super(false);

        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.configPanelProvider = configPanelProvider;
		this.krakenPluginManager = krakenPluginManager;
		this.krakenClient = krakenClient;
		this.discordAuth = discordAuth;
		this.credentialManager = credentialManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }
        });


        JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        topPanel.add(searchBar, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        mainPanel = new FixedWidthPanel();
        mainPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
        mainPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel northPanel = new FixedWidthPanel();
        northPanel.setLayout(new BorderLayout());
        northPanel.add(mainPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

		JPanel discordPanel = new FixedWidthPanel();
		discordPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		discordPanel.setLayout(new BorderLayout(0, BORDER_OFFSET));
		discordButton = new JButton(SIGN_IN_DISCORD_BUTTON_TEXT);
		discordButton.setIcon(discordIcon);
		discordButton.setIconTextGap(8);
		discordPanel.add(discordButton);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(discordPanel, BorderLayout.CENTER);
		bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(bottomPanel, BorderLayout.SOUTH);

		this.muxer = new MultiplexingPluginPanel(this) {
			@Override
			protected void onAdd(PluginPanel p) {
				eventBus.register(p);
			}

			@Override
			protected void onRemove(PluginPanel p) {
				eventBus.unregister(p);
			}
		};

		// TODO This flow should ideally start on plugin load not when user first opens the PluginListPanel
		startAuthFlow();
    }

	/**
	 * Starts the general Auth flow for a user. It goes as follows:
	 * - Attempt to see if creds are already on disk
	 * 	- Yes: authenticate with creds against Cognito via Kraken API
	 * 		- Success: Update button action listener and load proper JAR's for purchased plugins
	 * 		- Failure: Update button to re-link discord, delete creds from disk, warn user.
	 * - No: Update button to direct user through Discord oauth flow to create user in cognito & write creds to disk.
	 */
	public void startAuthFlow() {
		CognitoUser user = credentialManager.loadUserCredentials();
		if(user == null) {
			// The user has not gone through the OAuth 2.0 flow with discord yet.
			discordButton.setText(SIGN_IN_DISCORD_BUTTON_TEXT);
			discordButton.addActionListener(discordOAuthFlow());
		} else if(user.getDiscordId() == null || user.getCredentials() == null || user.getDiscordUsername() == null) {
			// Local user data is corrupted somehow we need to re-auth
			log.warn("User data is corrupted re-auth with discord required.");
			credentialManager.removeUserCredentials();
			discordButton.setText(SIGN_IN_DISCORD_BUTTON_TEXT);
			discordButton.addActionListener(discordOAuthFlow());
		} else {
			// The user has linked their discord, attempt to authenticate creds on disk.
			// TODO It would be nice if krakenClient.authenticate returned new creds + discord user info
			// because the next request we need to make will be to determine which plugin JAR's we need to download.

			// TODO Figure out how to lock API gateway routes behind authenticated cognito users.

			CognitoUser newCreds = krakenClient.authenticate(user);
			if(newCreds != null) {
				log.info("User: {} has been successfully authenticated.", user.getDiscordUsername());
				credentialManager.persistUserCredentials(newCreds);
				discordButton.addActionListener(e -> disconnectDiscord());
				discordButton.setText(DISCONNECT_DISCORD_BUTTON_TEXT);
			} else {
				log.info("User failed auth. Removing discord association.");
				// User failed auth. Remove credentials
				discordButton.setText(SIGN_IN_DISCORD_BUTTON_TEXT);
				discordButton.addActionListener(discordOAuthFlow());
				credentialManager.removeUserCredentials();
			}
		}
	}

	/**
	 * A flow which goes through the discord OAuth flow to get an access token and discord user info. User info
	 * is used to create a new Cognito user and get cognito credentials to store on disk for future auth.
	 * @return ActionListener
	 */
	private ActionListener discordOAuthFlow() {
		return e -> {
            log.info("Starting OAuth 2.0 flow with Discord.");
            discordAuth.getDiscordUser()
                .thenAccept(user -> {
                    log.info("Discord OAuth flow completed. User email = {}", user.getEmail());

					// TODO Here check if the user is in cognito but is disabled (need new api route for this)
					//  if they are disabled re-enable them else continue to create a new user.

                    // Now create the user with cognito via Kraken API
                    CognitoUser cognitoUser = krakenClient.createUser(new CreateUserRequest(user));
                    log.info("Created cognito user with id: {}", user.getId());
                    credentialManager.persistUserCredentials(cognitoUser);
                    discordButton.setText(DISCONNECT_DISCORD_BUTTON_TEXT);
                })
                .exceptionally(throwable -> {
                    log.error("Authentication failed: {}", throwable.getMessage());
                    return null;
                });
        };
    }

	/**
	 * A flow which disassociates a users Discord account from Kraken. The user account will be removed from Cognito
	 * and credentials will be removed from disk.
	 * @return
	 */
	private void disconnectDiscord() {
		// TODO Implement this function soon
		//  VERY IMPORTANT do NOT delete user data from cognito only disable them. If you delete the user when they
		//  disconnect it will also remove their purchased plugins from their account. If the user re-does discord auth AND they are in
		//  cognito but disabled simply re-enable them.
	}

	/**
	 * Rebuilds the Kraken plugin list when changes have been made to a plugin via the KrakenPluginManager.
	 */
    public void rebuildPluginList() {
		final List<String> pinnedPlugins = getPinnedPluginNames();

		// populate Kraken plugin with all non-hidden plugins
		pluginList = Stream.concat(
			fakePlugins.stream(),
			pluginManager.getPlugins().stream()
				.filter(plugin -> krakenPluginManager.getPluginMap().get(plugin.getName()) != null)
				.map(plugin ->
				{
					PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
					Config config = pluginManager.getPluginConfigProxy(plugin);
					ConfigDescriptor configDescriptor = config == null ? null : configManager.getConfigDescriptor(config);
					List<String> conflicts = pluginManager.conflictsForPlugin(plugin).stream()
						.map(Plugin::getName)
						.collect(Collectors.toList());

					return new PluginMetadata(
						descriptor.name(),
						descriptor.description(),
						descriptor.tags(),
						plugin,
						config,
						configDescriptor,
						conflicts);
				})
		)
			.map(desc ->
			{
				KrakenPluginListItem listItem;
				// Always pin Kraken Plugins to the top and remove the "pin" star icon. TODO doesn't look quite right.
				if(desc.getName().equals("Kraken Plugins")) {
					listItem = new KrakenPluginListItem(this, desc, true);
					listItem.setPinned(true);
					return listItem;
				}

				listItem = new KrakenPluginListItem(this, desc, true);
				listItem.setPinned(pinnedPlugins.contains(desc.getName()));
				return listItem;
			})
			.sorted(Comparator.comparing(p -> p.getPluginConfig().getName()))
			.collect(Collectors.toList());

		mainPanel.removeAll();
		refresh();
	}

	/**
	 * Refreshes the list of Kraken plugins.
	 */
	void refresh() {
		pluginList.forEach(listItem -> {
			final Plugin plugin = listItem.getPluginConfig().getPlugin();
			if (plugin != null) {
				listItem.setPluginEnabled(pluginManager.isPluginEnabled(plugin));
			}
		});

		int scrollBarPosition = scrollPane.getVerticalScrollBar().getValue();

		onSearchBarChanged();
		searchBar.requestFocusInWindow();
		validate();

		scrollPane.getVerticalScrollBar().setValue(scrollBarPosition);
	}

    private void onSearchBarChanged() {
        pluginList.forEach(mainPanel::remove);
        PluginSearch.search(pluginList, searchBar.getText()).forEach(mainPanel::add);
        revalidate();
    }

	private List<String> getPinnedPluginNames() {
		final String config = configManager.getConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY);
		if (config == null) {
			return Collections.emptyList();
		}

		return Text.fromCSV(config);
	}

	/**
	 * Generates the JPanel UI elements for a plugins configuration when the "gear" icon is clicked.
	 * @param metadata PluginMetadata Metadata from the plugin's descriptor annotation.
	 */
	public void openConfigurationPanel(PluginMetadata metadata) {
		// Note: Although guice creates this it will create a new ConfigPanel object each time.
		ConfigPanel panel = configPanelProvider.get();
		panel.init(metadata);
		muxer.pushState(this);
		muxer.pushState(panel);
	}

	public void savePinnedPlugins() {
		final String value = pluginList.stream()
			.filter(KrakenPluginListItem::isPinned)
			.map(p -> p.getPluginConfig().getName())
			.collect(Collectors.joining(","));

		configManager.setConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY, value);
	}

	/**
	 * Starts a plugin registering it with the EventBus.
	 * @param plugin
	 */
    public void startPlugin(Plugin plugin) {
        pluginManager.setPluginEnabled(plugin, true);
        try {
            pluginManager.startPlugin(plugin);
        } catch (PluginInstantiationException e) {
            log.error("Failed to start plugin: {}. Error = {}", plugin.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

	/**
	 * Stops a plugin de-registering it from the EventBus.
	 * @param plugin
	 */
    public void stopPlugin(Plugin plugin) {
        pluginManager.setPluginEnabled(plugin, false);
        try {
            pluginManager.stopPlugin(plugin);
        } catch (PluginInstantiationException e) {
            log.error("Failed to stop plugin: {}. Error = {}", plugin.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(PANEL_WIDTH + SCROLLBAR_WIDTH, super.getPreferredSize().height);
	}

	@Override
	public void onActivate() {
		super.onActivate();
		if (searchBar.getParent() != null) {
			searchBar.requestFocusInWindow();
		}
	}
}
