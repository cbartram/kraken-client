package com.kraken.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.kraken.api.model.CognitoUser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
@Singleton
public class KrakenCredentialManager {
    private static final String KRAKEN_DIR = "kraken";
    private static final String CREDS_FILE = "creds.json";
    private final ObjectMapper mapper;


    public KrakenCredentialManager() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Saves a set of user (Cognito) credentials to disk.
     * @param cognitoUser The user information to save: id, username, email from discord and the access token and refresh token.
     */
    public void persistUserCredentials(CognitoUser cognitoUser) {
        File krakenDir = new File(RUNELITE_DIR, KRAKEN_DIR);

        if (!krakenDir.exists()) {
            try {
                Files.createDirectories(krakenDir.toPath());
            } catch (IOException e) {
                log.error("IOException thrown while trying to create the Kraken directory at path: {}. Error = {}", krakenDir.getPath(), e.getMessage());
                e.printStackTrace();
            }
        }

        Path credsFilePath = Paths.get(krakenDir.toString(), CREDS_FILE);
        File credsFile = credsFilePath.toFile();

        if (!credsFile.exists()) {
            try {
                Files.createFile(credsFilePath);
            } catch (IOException e) {
                // Handle exception, e.g., log the error
                log.error("IOException thrown while attempting to create creds.json file in: {}. Error = {}", krakenDir.getPath(), e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            if(cognitoUser.getDiscordUsername() == null || cognitoUser.getDiscordId() == null || cognitoUser.getCredentials() == null) {
                log.info("Cognito user data is null. Skipping persist.");
                return;
            }
            log.info("User info persisted to disk.");
            mapper.writeValue(credsFile, cognitoUser);
        } catch (IOException e) {
            log.error("Failed writing creds to JSON file. Path = {}. Error = {}", credsFile.getPath(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads a set of Cognito credentials and user information (discord id, username, email) from disk.
     * @return CognitoCredentials an access token and refresh token for Cognito.
     */
    public CognitoUser loadUserCredentials() {
        Path credsFilePath = Paths.get(RUNELITE_DIR.getPath(), KRAKEN_DIR, CREDS_FILE);
        File credsFile = credsFilePath.toFile();

        if (credsFile.exists() && credsFile.length() > 0) {
            try {
                return mapper.readValue(credsFile, CognitoUser.class);
            } catch (IOException e) {
                log.error("IOException thrown while attempting to load user credentials. Error = {}", e.getMessage());
                e.printStackTrace();
            }
        }
        log.warn("No credential file exists at: {} to load or file is empty.", credsFilePath);
        return null;
    }

}
