package com.kraken.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
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
     * @param cognitoCredentials CognitoCredentials the access token and refresh token to save.
     */
    public void persistUserCredentials(CognitoCredentials cognitoCredentials) {
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
            mapper.writeValue(credsFile, cognitoCredentials);
        } catch (IOException e) {
            log.error("Failed writing creds to JSON file. Path = {}. Error = {}", credsFile.getPath(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads a set of Cognito credentials from disk.
     * @return CognitoCredentials an access token and refresh token for Cognito.
     */
    public CognitoCredentials loadUserCredentials() {
        Path credsFilePath = Paths.get(RUNELITE_DIR.getPath(), KRAKEN_DIR, CREDS_FILE);
        File credsFile = credsFilePath.toFile();

        if (credsFile.exists()) {
            try {
                return mapper.readValue(credsFile, CognitoCredentials.class);
            } catch (IOException e) {
                log.error("IOException thrown while attempting to load user credentials. Error = {}", e.getMessage());
                e.printStackTrace();
            }
        }
        log.warn("No credential file exists at: {} to load.", credsFilePath);
        return null;
    }

    /**
     * Removes a set of user credentials from disk.
     */
    public void removeUserCredentials() {
        try {
            Path credentialsFile = Paths.get(RUNELITE_DIR.getPath(), KRAKEN_DIR, CREDS_FILE);

            if (Files.exists(credentialsFile)) {
                Files.delete(credentialsFile);
                log.info("Credentials file successfully deleted");
            } else {
                log.info("No credentials file found to delete");
            }
        } catch (IOException e) {
            log.error("Failed to delete credentials file", e);
        }
    }
}
