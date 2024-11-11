package com.kraken.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.model.*;
import com.kraken.api.model.CognitoAuth;
import com.kraken.api.model.DiscordTokenResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
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
     * Creates a presigned URL from S3 to download plugin JAR files.
     * @param credentials CognitoCredentials A set of cognito credentials including an access token for API access and id token
     *                    to validate user permissions.
     * @return HttpResponse HTTP response from API.
     */
    public Map<String, List<PreSignedURL>> createPresignedUrl(CognitoCredentials credentials) {
        // Strange but you must send id token to access the API. The access token is still required to lookup the authenticated
        // user on the backend to find purchased plugins.
        try {
            HttpResponse<String> res = sendRequestGeneric("POST", "/api/v1/plugin/create-presigned-url", credentials, credentials.getIdToken());
            return objectMapper.readValue(res.body(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("IOException thrown while attempting to make PUT API request to /api/v1/plugin/create-presigned-url. Error = {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates that a plugin license key is: not expired, connected to the correct hardware, and associated with the right account
     * @param request ValidateLicenseRequest request payload.
     * @return Map
     */
    public boolean validateLicense(ValidateLicenseRequest request) {
        try {
            HttpResponse<String> res = sendRequestGeneric("POST", "/api/v1/plugin/validate-license", request, request.getCredentials().getIdToken());
            Map<String, String> licenseKeyResponse = objectMapper.readValue(res.body(), new TypeReference<>() {});
            log.debug("License Key Response: keys = {}, values = {}", licenseKeyResponse.keySet(), licenseKeyResponse.values());
            if(licenseKeyResponse.get("error") == null) {
                return true;
            }
        } catch (IOException e) {
            log.error("IOException thrown while attempting to make PUT API request to /api/v1/plugin/validate-license Error = {}", e.getMessage());
        }
        return false;
    }

    /**
     * Makes a POST request to the Kraken API to create a new user in AWS Cognito.
     * @param request CreateUserRequest POJO which holds discord information about the user.
     * @return CognitoCredentials A set of credentials (access_token & refresh_token)
     */
    public CognitoUser createUser(@NonNull CreateUserRequest request) {
        return sendRequestGeneric("POST", "/api/v1/cognito/create-user", request, CognitoUser.class, null);
    }

    /**
     *  Authenticates a set of CognitoCredentials against AWS Cognito. If the user returned from this function
     *  are null it means that the user has failed auth. The credentials returned alongside the user data will always be
     *  a fresh OAuth access token.
     * @param request CognitoCredentials A set of credentials to authenticate. In this case only the refresh_token and discord id are required.
     * @return CognitoCredentials A set of credentials (access_token & refresh_token)
     */
    public CognitoUser authenticate(@NonNull CognitoAuth request) {
        return sendRequestGeneric("POST", "/api/v1/cognito/auth", request, CognitoUser.class, null);
    }


    /**
     * Refreshes a users session with a new refresh token.
     * @param request CognitoAuth the discord id of the user to refresh. The current refresh token is not needed.
     * @return
     */
    public CognitoCredentials refreshSession(@NonNull CognitoAuth request) {
        return sendRequestGeneric("POST", "/api/v1/cognito/refresh-session", request, CognitoCredentials.class, null);
    }

    /**
     * Performs very similar functionality to authenticate however, this does not authenticate a user. It only returns a
     * Cognito user if it exists. It will return disabled accounts as well.
     * @param discordId String the user's discord id.
     * @return
     */
    public CognitoUser getUser(@NonNull String discordId) {
        return sendRequestGeneric("GET", "/api/v1/cognito/get-user?discordId=" + discordId, null, CognitoUser.class, null);
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
            HttpResponse<String> response = sendRequestGeneric("PUT", "/api/v1/cognito/user-status",  "{\"discordId\":\"" + discordId + "\", \"accountEnabled\":" + accountEnabled + "}", null);
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
        DiscordTokenResponse res = sendRequestGeneric("POST", "/api/v1/discord/oauth", request, DiscordTokenResponse.class, null);
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
    private HttpResponse<String> sendRequestGeneric(@NonNull String method, @NonNull String path, String jsonBody, String accessToken) throws IOException, InterruptedException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(jsonBody));

        if(accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[{}] - {} - Status Code = {}", method, path, res.statusCode());

        if(res.statusCode() < 200 || res.statusCode() > 399) {
            log.error("Unexpected response code from: {} request to: {}, status code = {}, body: {}", method, path, res.statusCode(), res.body());
        }

        return res;
    }

    private HttpResponse<String> sendRequestGeneric(String method, String path, Object body, String accessToken) {
        try {
            if(body == null) {
                return sendRequestGeneric(method, path, null, accessToken);
            }
            return sendRequestGeneric(method, path, objectMapper.writeValueAsString(body), accessToken);
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

    private <T> T sendRequestGeneric(String method, String path, Object body, Class<T> deserializationClass, String accessToken)  {
        HttpResponse<String> response = sendRequestGeneric(method, path, body, accessToken);

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
