package com.kraken.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Map;


@Slf4j
@Singleton
public class KrakenClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "https://rog742w0fa.execute-api.us-east-1.amazonaws.com/prod";

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
    public CognitoUser createUser(@NonNull CreateUserRequest request) {
        return sendRequestGeneric("POST", "/api/v1/cognito/create-user", request, CognitoUser.class);
    }

    /**
     *  Authenticates a set of CognitoCredentials against AWS Cognito. If the user returned from this function
     *  are null it means that the user has failed auth. The credentials returned alongside the user data will always be
     *  a fresh OAuth access token.
     * @param request CognitoCredentials A set of credentials to authenticate. In this case only the refresh_token and discord id are required.
     * @return CognitoCredentials A set of credentials (access_token & refresh_token)
     */
    public CognitoUser authenticate(@NonNull CognitoUser request) {
        return sendRequestGeneric("POST", "/api/v1/cognito/auth", request, CognitoUser.class);
    }

    /**
     * Performs very similar functionality to authenticate however, this does not authenticate a user. It only returns a
     * Cognito user if it exists. It will return disabled accounts as well.
     * @param discordId String the user's discord id.
     * @return
     */
    public CognitoUser getUser(@NonNull String discordId) {
        return sendRequestGeneric("GET", "/api/v1/cognito/get-user?discordId=" + discordId, null, CognitoUser.class);
    }

    /**
     * Returns 2 boolean values: "userExists" and "userEnabled" representing whether the user with the provided discord id
     * exists in Cognito and is enabled or not.
     * @param discordId String the users discord id
     * @return
     */
    public Map<String, Boolean> doesUserExist(@NonNull String discordId) {
        try {
            HttpResponse<String> response = sendRequestGeneric("GET", "/api/v1/cognito/user-exists?discordId=" + discordId, null);
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make POST API request to /api/v1/cognito/user-exists. Error = {}", e.getMessage());
            return null;
        }
    }

    /**
     * Updates a users account status to either disabled or enabled.
     * @param discordId String the discord id for the account to update
     * @param accountEnabled Boolean the status of the account.
     * @return A map which contains one key/value pair. The key is "accountEnabled" and the value is a boolean value of
     * true or false depending on if this method is being used to enable or disable an account
     */
    public Map<String, Boolean> updateUserStatus(@NonNull String discordId, boolean accountEnabled) {
        try {
            HttpResponse<String> response = sendRequestGeneric("PUT", "/api/v1/cognito/user-status",  "{\"discordId\":\"" + discordId + "\", \"accountEnabled\":" + accountEnabled + "}");
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException | InterruptedException e) {
            log.error("IOException thrown while attempting to make PUT API request to /api/v1/cognito/user-status. Error = {}", e.getMessage());
            return null;
        }
    }

    /**
     * Posts a discord code from the OAuth callback to the Kraken API to exchange for a discord OAuth token (access_token, refresh_token, etc...).
     * @param request DiscordOAuthRequest the callback code from the URL params to post for an access token.
     * @return DiscordTokenResponse a set of discord credentials (access token, refresh token, expiration timestamp etc...)
     */
    public DiscordTokenResponse postDiscordOAuthCode(@NonNull DiscordOAuthRequest request) {
        DiscordTokenResponse res = sendRequestGeneric("POST", "/api/v1/discord/oauth", request, DiscordTokenResponse.class);
        if(res != null) {
            return res;
        }
        log.error("Discord token response from API call is null.");
        return null;
    }

    /**
     * Sends a POST request to the provided endpoint with a generic pre-encoded JSON string body. This is useful
     * when we don't want to create a class for an object to send in a simple request.
     * @param path
     * @param jsonBody String the json body to send in the POST request.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private HttpResponse<String> sendRequestGeneric(String method, String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[{}] - {} - Status Code = {}", method, path, res.statusCode());

        if(res.statusCode() < 200 || res.statusCode() > 399) {
            log.error("Unexpected response code from: {} request to: {}, status code = {}", method, path, res.statusCode());
        }

        return res;
    }

    private HttpResponse<String> sendRequestGeneric(String method, String path, Object body) {
        try {
            if(body == null) {
                return sendRequestGeneric(method, path, null);
            }
            return sendRequestGeneric(method, path, objectMapper.writeValueAsString(body));
        } catch(JsonProcessingException e) {
            log.error("Failed to write body of request to json. Error = {}", e.getMessage());
            e.printStackTrace();
        } catch(IOException e) {
            log.error("IOException thrown while attempting to send request: method={}, path={}, error={}", method, path, e.getMessage());
            e.printStackTrace();
        } catch(InterruptedException e) {
            log.error("Interrupted exception thrown while attempting to send request: method={}, path={}, error={}", method, path, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private <T> T sendRequestGeneric(String method, String path, Object body, Class<T> deserializationClass)  {
        HttpResponse<String> response = sendRequestGeneric(method, path, body);

        if(response == null) {
            return null;
        }

        try {
            if(response.body() != null) {
                return objectMapper.readValue(response.body(), deserializationClass);
            }
            log.error("Cannot deserialize null response body into class: {}", deserializationClass.getName());
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing response to POJO. Status code = " + response.statusCode(), e);
        }
        return null;
    }
}
