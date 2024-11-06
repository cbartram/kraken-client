package com.kraken.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String CLIENT_SECRET = "your-client-secret";
    private static final int PORT = 8080;
    private static final String REDIRECT_URI = "http://localhost:" + PORT + "/discord/oauth";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private HttpServer server;
    private final CountDownLatch authorizationLatch = new CountDownLatch(1);
    private DiscordTokenResponse tokenResponse;
    private String authCode;

    public DiscordAuth() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<DiscordUser> authenticate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                startLocalServer();
                openBrowser();
                authorizationLatch.await(); // Wait for auth callback

                // Exchange authCode with Kraken-API for access token
                try {
                    tokenResponse = getDiscordAccessToken(authCode);
                    log.info("Token Response: {}", tokenResponse);
                } catch(InterruptedException e) {
                    log.error("Failed to exchange access code for discord access token. Error = {}", e.getMessage());
                    e.printStackTrace();
                }
                stopLocalServer();

                // Get user info
                return getDiscordUserInfo(tokenResponse.getAccessToken());
            } catch (Exception e) {
                log.error("Authentication Failed: {}", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Discord Authentication failed: " + e.getMessage());
            }
        });
    }


    // TODO Write refresh function when user starts client up and are authed then try to refresh.


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

    private DiscordTokenResponse getDiscordAccessToken(String authCode) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://rog742w0fa.execute-api.us-east-1.amazonaws.com/default/api/v1/discord-oauth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"code\": \"" + authCode + "\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), DiscordTokenResponse.class);
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
