package com.kraken.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;


@Singleton
public class KrakenApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "https://rog742w0fa.execute-api.us-east-1.amazonaws.com/default";

    @Inject
    public KrakenApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<HttpResponse<String>> createUser(CreateUserRequest request) {
        return sendPostRequest("/api/v1/cognito/create-user", request);
    }

    public CompletableFuture<HttpResponse<String>> authenticate(AuthRequest request) {
        return sendPostRequest("/api/v1/cognito/auth", request);
    }

    public CompletableFuture<HttpResponse<String>> handleDiscordOAuth(DiscordOAuthRequest request) {
        return sendPostRequest("/api/v1/discord/oauth", request);
    }

    private CompletableFuture<HttpResponse<String>> sendPostRequest(String path, Object body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // Builder pattern for custom configuration
    public static class Builder {
        private String baseUrl;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public KrakenApiClient build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("Base URL is required");
            }
            return new KrakenApiClient();
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
