package com.kraken.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.auth.DiscordTokenResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Slf4j
@Singleton
public class KrakenClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "https://rog742w0fa.execute-api.us-east-1.amazonaws.com/default";

    @Inject
    public KrakenClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Makes a POST request to the Kraken API to create a new user in AWS Cognito.
     * @param request CreateUserRequest POJO which holds discord information about the user.
     * @return CognitoCredentials A set of credentials (access_token & refresh_token)
     */
    public CognitoCredentials createUser(@NonNull CreateUserRequest request) {
        try {
            return sendPostRequest("/api/v1/cognito/create-user", request, CognitoCredentials.class);
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make API request to /api/v1/cognito/create-user. Error = {}", e.getMessage());
            return null;
        }
    }

    /**
     *  Authenticates a set of CognitoCredentials against AWS Cognito. If the credentials returned from this function
     *  are null it means that the user has failed auth.
     * @param request CognitoCredentials A set of credentials to authenticate. In this case only the refresh_token is required.
     * @return CognitoCredentials A set of credentials (access_token & refresh_token)
     */
    public CognitoCredentials authenticate(@NonNull CognitoCredentials request) {
        try {
            return sendPostRequest("/api/v1/cognito/auth", request, CognitoCredentials.class);
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make API request to /api/v1/cognito/auth. Error = {}", e.getMessage());
            return null;
        }
    }

    /**
     * Posts a discord code from the OAuth callback to the Kraken API to exchange for a discord OAuth token (access_token, refresh_token, etc...).
     * @param request DiscordOAuthRequest the callback code from the URL params to post for an access token.
     * @return DiscordTokenResponse a set of discord credentials (access token, refresh token, expiration timestamp etc...)
     */
    public DiscordTokenResponse postDiscordOAuthCode(@NonNull DiscordOAuthRequest request) {
        try {
            return sendPostRequest("/api/v1/discord/oauth", request, DiscordTokenResponse.class);
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make API request to /api/v1/discord/oauth. Error = {}", e.getMessage());
            return null;
        }
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
