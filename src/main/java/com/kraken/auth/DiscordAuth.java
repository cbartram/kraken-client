package com.kraken.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kraken.api.DiscordOAuthRequest;
import com.kraken.api.KrakenClient;
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
    private final KrakenClient krakenClient;

    private HttpServer server;
    private DiscordTokenResponse tokenResponse;
    private String authCode;

    @Inject
    public DiscordAuth(KrakenClient krakenClient) {
        this.krakenClient = krakenClient;
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
                tokenResponse = krakenClient.postDiscordOAuthCode(new DiscordOAuthRequest(authCode));
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
            String response = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>Kraken Authentication Successful</title>\n" +
                    "    <style>\n" +
                    "        body {\n" +
                    "            font-family: Arial, sans-serif;\n" +
                    "            background-color: #7289DA;\n" +
                    "            color: #FFFFFF;\n" +
                    "            display: flex;\n" +
                    "            justify-content: center;\n" +
                    "            align-items: center;\n" +
                    "            height: 100vh;\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            text-align: center;\n" +
                    "            padding: 2rem;\n" +
                    "            background-color: #2C2F33;\n" +
                    "            border-radius: 8px;\n" +
                    "            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\n" +
                    "            max-width: 500px;\n" +
                    "            width: 90%;\n" +
                    "        }\n" +
                    "        h1 {\n" +
                    "            font-size: 2.5rem;\n" +
                    "            margin-bottom: 1rem;\n" +
                    "        }\n" +
                    "        p {\n" +
                    "            font-size: 1.2rem;\n" +
                    "            margin-bottom: 2rem;\n" +
                    "        }\n" +
                    "        .logo {\n" +
                    "            max-width: 150px;\n" +
                    "            margin-bottom: 2rem;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n" +
                    "    <img src=\"https://i.ibb.co/NSFyTLL/kraken.png\" alt=\"Kraken Logo\" class=\"logo\">\n" +
                    "    <h1>Authentication Successful!</h1>\n" +
                    "    <p>Your Kraken account has been successfully authenticated with Discord.</p>\n" +
                    "    <p>You can now continue using the Kraken client with your Discord credentials.</p>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>";
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

    /**
     * Retrieves user information from Discord such as id, username, and email.
     * @param accessToken OAuth access token granted from discord OAuth flow.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
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
