package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.*;
import com.kraken.api.model.CognitoUser;
import com.kraken.api.model.CreateUserRequest;
import com.kraken.api.model.PreSignedURL;
import com.kraken.api.model.CognitoAuth;
import com.kraken.api.model.DiscordAuth;
import com.kraken.panel.KrakenPluginListPanel;
import com.kraken.panel.RootPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Kraken Plugins",
        description = "Loads the Kraken Plugins.",
        hidden = false,
        tags = {"kraken", "plugin", "loader"}
)
public class KrakenLoaderPlugin extends Plugin {

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
	private Provider<KrakenPluginListPanel> pluginListPanelProvider;

    @Inject
    private Provider<RootPanel> rootPanelProvider;

    @Inject
    private KrakenPluginManager krakenPluginManager;

    @Inject
    private KrakenClient krakenClient;

    @Inject
    private DiscordAuth discordAuth;

    @Inject
    private KrakenCredentialManager credentialManager;

    private NavigationButton navButton;
    private static final String DISCONNECT_DISCORD_BUTTON_TEXT = "Disconnect Discord";
    private static final String SIGN_IN_DISCORD_BUTTON_TEXT = "Sign-in with Discord";

    @Override
    protected void startUp() {
        RootPanel panelRoot = rootPanelProvider.get();
        KrakenPluginListPanel panel = pluginListPanelProvider.get();
        boolean userAuthenticated = startAuthFlow(panel.getDiscordButton());

        if(userAuthenticated) {
            CognitoUser user = credentialManager.loadUserCredentials();
            Map<String, List<PreSignedURL>> preSignedUrls = krakenClient.createPresignedUrl(user.getCredentials());
            for(PreSignedURL url : preSignedUrls.get("urls")) {
                krakenPluginManager.loadPlugin(url);
            }

            // Start all loaded plugins
            krakenPluginManager.startKrakenPlugins(user);
        }

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

    private void resetDiscordButton(JButton discordButton) {
        discordButton.setText(SIGN_IN_DISCORD_BUTTON_TEXT);
        discordButton.addActionListener(discordOAuthFlow());
    }

    /**
     * Starts the general Auth flow for a user. It goes as follows:
     * - Attempt to see if creds are already on disk
     * 	- Yes: authenticate with creds against Cognito via Kraken API
     * 		- Success: Update button action listener and load proper JAR's for purchased plugins
     * 		- Failure: Update button to re-link discord, delete creds from disk, warn user.
     * - No: Update button to direct user through Discord oauth flow to create user in cognito & write creds to disk.
     */
    public boolean startAuthFlow(JButton discordButton) {
        CognitoUser user = credentialManager.loadUserCredentials();
        if(user == null) {
            log.info("No creds on disk. User has not gone through discord. OAuth 2.0 flow.");
            // The user has not gone through the OAuth 2.0 flow with discord yet.
        } else {
            // The user has linked their discord, attempt to authenticate creds on disk.
            CognitoUser authUser = krakenClient.authenticate(new CognitoAuth(user.getDiscordId(), user.getCredentials().getRefreshToken()));
            if(authUser.getDiscordId() != null && authUser.getDiscordUsername() != null) {
                credentialManager.persistUserCredentials(authUser);
                discordButton.addActionListener(e -> disconnectDiscord(authUser, discordButton));
                discordButton.setText(DISCONNECT_DISCORD_BUTTON_TEXT);
                log.info("User: {} has been successfully authenticated.", user.getDiscordUsername());
                return true;
            }
            log.info("User auth failed. Disconnecting discord.");
        }
        resetDiscordButton(discordButton);
        return false;
    }

    /**
     * A flow which goes through the discord OAuth flow to get an access token and discord user info. User info
     * is used to create a new Cognito user and get cognito credentials to store on disk for future auth.
     * @return ActionListener
     */
    private ActionListener discordOAuthFlow() {
        return e -> {
            log.info("Starting OAuth 2.0 flow with Discord.");
            JButton btn = (JButton) e.getSource();
            discordAuth.getDiscordUser()
                    .thenAccept(user -> {
                        log.info("Discord OAuth flow completed. User email = {}. Creating new cognito user.", user.getEmail());
                        CognitoUser cognitoUser = krakenClient.createUser(new CreateUserRequest(user, HardwareUtils.getHardwareId()));
                        credentialManager.persistUserCredentials(cognitoUser);
                        btn.addActionListener(evt -> disconnectDiscord(cognitoUser, btn));
                        btn.setText(DISCONNECT_DISCORD_BUTTON_TEXT);
                    })
                    .exceptionally(throwable -> {
                        log.error("Authentication failed: {}", throwable.getMessage());
                        throwable.printStackTrace();
                        return null;
                    });
        };
    }

    /**
     * A flow which disassociates a users Discord account from Kraken. The user account will be disabled in Cognito
     * and credentials will be removed from disk. The account can be re-enabled by following the normal discord OAuth flow.
     * Note: This NEVER deletes a user account as that would also remove data around the users purchased plugins.
     */
    private void disconnectDiscord(CognitoUser user, JButton discordButton) {
        krakenClient.updateUserStatus(user.getDiscordId(), false);
        user.setAccountEnabled(false);
        credentialManager.persistUserCredentials(user);
        resetDiscordButton(discordButton);
    }


    @Override
    protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
    }

}