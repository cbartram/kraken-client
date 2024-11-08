package com.kraken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.CognitoUser;
import com.kraken.api.CreateUserRequest;
import com.kraken.api.KrakenClient;
import com.kraken.api.KrakenCredentialManager;
import com.kraken.auth.DiscordAuth;
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
import java.util.Map;

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
        startAuthFlow(panel.getDiscordButton());

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
    public void startAuthFlow(JButton discordButton) {
        CognitoUser user = credentialManager.loadUserCredentials();

        if(user == null) {
            log.info("No creds on disk. User has not gone through discord. OAuth 2.0 flow.");
            // The user has not gone through the OAuth 2.0 flow with discord yet.
            resetDiscordButton(discordButton);
        } else if(user.getDiscordId() == null || user.getCredentials() == null || user.getDiscordUsername() == null) {
            // Local user data is corrupted somehow need to re-auth
            log.info("User data is corrupted re-auth with discord required.");
            credentialManager.removeUserCredentials();
            resetDiscordButton(discordButton);
        } else {
            // The user has linked their discord, attempt to authenticate creds on disk.
            // TODO Figure out how to lock API gateway routes behind authenticated cognito users.
            CognitoUser newCreds = krakenClient.authenticate(user);
            if(newCreds.getDiscordId() != null && newCreds.getDiscordUsername() != null) {
                if(!newCreds.isAccountEnabled()) {
                    log.info("User: {} exists but has a disabled account. Saving creds and re-enabling.", user.getDiscordUsername());
                    krakenClient.updateUserStatus(newCreds.getDiscordId(), true);
                }

                log.info("User: {} has been successfully authenticated.", user.getDiscordUsername());
                credentialManager.persistUserCredentials(newCreds);
                discordButton.addActionListener(e -> disconnectDiscord(newCreds.getDiscordId(), discordButton));
                discordButton.setText(DISCONNECT_DISCORD_BUTTON_TEXT);
            } else {
                log.info("User failed auth. Removing discord association.");
                // User failed auth. Remove credentials
                resetDiscordButton(discordButton);
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
            JButton btn = (JButton) e.getSource();

            discordAuth.getDiscordUser()
                    .thenAccept(user -> {
                        CognitoUser cognitoUser;
                        log.info("Discord OAuth flow completed. User email = {}", user.getEmail());

                        // Check if the user is in cognito but is disabled if they are disabled re-enable them else
                        // continue to create a new user.
                        Map<String, Boolean> potentialUser = krakenClient.doesUserExist(user.getId());

                        // TODO I don't like this generic map here. Instead just do getUser and check if userid || name is null to determine existence.
                        //   You can also check .accountEnabled() on a user to determine if it exists and is enabled or not.
                        if(potentialUser.get("userExists") && !potentialUser.get("userEnabled")) {
                            log.info("User already exists in cognito but is disabled. Re-enabling user.");
                            Map<String, Boolean> response = krakenClient.updateUserStatus(user.getId(), true);
                            log.info("Update user status response accountEnabled: {}", response.get("accountEnabled"));
                            cognitoUser = krakenClient.getUser(user.getId());
                            credentialManager.persistUserCredentials(cognitoUser);
                        } else if(potentialUser.get("userExists") && potentialUser.get("userEnabled")) {
                            log.info("User exists and is enabled. Starting standard auth process.");
                            // Case where user auth failed so creds were removed but user still exists and is enabled in cognito
                            // No need to re-create the user simply retry auth.
                            CognitoUser tmpUser = krakenClient.getUser(user.getId());

                            if(tmpUser.getDiscordId() != null) {
                                cognitoUser = krakenClient.authenticate(tmpUser);
                                if (cognitoUser.getDiscordId() == null || cognitoUser.getDiscordUsername() == null) {
                                    log.error("Failed to authenticate user: {}.", user.getEmail());
                                    resetDiscordButton(btn);
                                    return;
                                }
                                log.info("Successfully authenticated user with username: {}", tmpUser.getDiscordUsername());
                                credentialManager.persistUserCredentials(cognitoUser);
                                return;
                            }
                            log.error("Failed to get user.");
                            return;
                        } else {
                            // Now create the user with cognito via Kraken API
                            cognitoUser = krakenClient.createUser(new CreateUserRequest(user));
                            log.info("Created cognito user with id: {}", user.getId());
                            credentialManager.persistUserCredentials(cognitoUser);
                        }
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
    private void disconnectDiscord(String discordId, JButton discordButton) {
        krakenClient.updateUserStatus(discordId, false);
        credentialManager.removeUserCredentials();
        resetDiscordButton(discordButton);
    }


    @Override
    protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
    }

}