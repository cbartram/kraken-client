package com.kraken.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kraken.api.DiscordOAuthRequest;
import com.kraken.api.KrakenApiClient;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


@Slf4j
public class DiscordAuth {
    private static final String CLIENT_ID = "1303515055777648640";
    private static final int PORT = 8080;
    private static final String REDIRECT_URI = "http://localhost:" + PORT + "/discord/oauth";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CountDownLatch authorizationLatch = new CountDownLatch(1);
    private final KrakenApiClient krakenApiClient;

    private HttpServer server;
    private DiscordTokenResponse tokenResponse;
    private String authCode;

    @Inject
    public DiscordAuth(KrakenApiClient krakenApiClient) {
        this.krakenApiClient = krakenApiClient;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<DiscordUser> getDiscordUser() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                startLocalServer();
                openBrowser();
                authorizationLatch.await(); // Wait for auth callback
                stopLocalServer();

                // Exchange authCode with Kraken-API for access token
                tokenResponse = krakenApiClient.postDiscordOAuthCode(new DiscordOAuthRequest(authCode));
                log.info("Token Response: {}", tokenResponse);

                // Get user info
                return getDiscordUserInfo(tokenResponse.getAccessToken());
            } catch (Exception e) {
                log.error("Authentication Failed: {}", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Discord Authentication failed: " + e.getMessage());
            }
        });
    }

    private void startLocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/discord/oauth", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            authCode = query.substring(query.indexOf("code=") + 5);

            // Send success response to browser
            String response = "<html><body><h1>Authentication successful!</h1><p>You can close this window.</p></body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            authorizationLatch.countDown();
        });
        server.setExecutor(null);
        server.start();
    }

    private void stopLocalServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void openBrowser() throws IOException {
        String url = String.format("https://discord.com/oauth2/authorize" +
                        "?client_id=%s" +
                        "&redirect_uri=%s" +
                        "&response_type=code" +
                        "&scope=%s",
                CLIENT_ID,
                URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8),
                URLEncoder.encode("identify email", StandardCharsets.UTF_8));

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
        } else {
            throw new IOException("Desktop browser open is not supported on user machine.");
        }
    }

    private DiscordUser getDiscordUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/users/@me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), DiscordUser.class);
    }
}
