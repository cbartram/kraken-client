package com.kraken.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class DiscordTokenResponse {

    private String message;

    @JsonAlias("access_token")
    private String accessToken;

    @JsonAlias("token_type")
    private String tokenType;

    @JsonAlias("expires_in")
    private Integer expiresIn;

    @JsonAlias("refresh_token")
    private String refreshToken;

    private String scope;
}