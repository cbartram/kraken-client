package com.kraken.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.auth.DiscordTokenResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static net.runelite.client.RuneLite.RUNELITE_DIR;


@Slf4j
@Singleton
public class KrakenApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "https://rog742w0fa.execute-api.us-east-1.amazonaws.com/default";
    private static final String KRAKEN_DIR = "kraken";
    private static final String CREDS_FILE = "creds.json";

    @Inject
    public KrakenApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CognitoCredentials createUser(CreateUserRequest request) {
        try {
            return sendPostRequest("/api/v1/cognito/create-user", request, CognitoCredentials.class);
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make API request to /api/v1/cognito/create-user. Error = {}", e.getMessage());
            return null;
        }
    }

//    public CompletableFuture<HttpResponse<String>> authenticate(CognitoCredentials request) {
//        return sendPostRequest("/api/v1/cognito/auth", request);
//    }
//
    public DiscordTokenResponse postDiscordOAuthCode(DiscordOAuthRequest request) {
        try {
            return sendPostRequest("/api/v1/discord/oauth", request, DiscordTokenResponse.class);
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make API request to /api/v1/discord/oauth. Error = {}", e.getMessage());
            return null;
        }
    }

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
            objectMapper.writeValue(credsFile, cognitoCredentials);
        } catch (IOException e) {
            log.error("Failed writing creds to JSON file. Path = {}. Error = {}", credsFile.getPath(), e.getMessage());
            e.printStackTrace();
        }
    }

    public CognitoCredentials loadUserCredentials() {
        Path credsFilePath = Paths.get(RUNELITE_DIR.getPath(), KRAKEN_DIR, CREDS_FILE);
        File credsFile = credsFilePath.toFile();

        if (credsFile.exists()) {
            try {
                return objectMapper.readValue(credsFile, CognitoCredentials.class);
            } catch (IOException e) {
                log.error("IOException thrown while attempting to load user credentials. Error = {}", e.getMessage());
                e.printStackTrace();
            }
        }
        log.warn("No credential file exists at: {} to load.", credsFilePath);
        return null;
    }

    private <T> T sendPostRequest(String path, Object body, Class<T> deserializationClass) throws IOException, RuntimeException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        try {
            return objectMapper.readValue(res.body(), deserializationClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing response to POJO. Status code = " + res.statusCode(), e);
        }
    }
}
