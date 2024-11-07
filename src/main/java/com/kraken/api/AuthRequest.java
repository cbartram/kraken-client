package com.kraken.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthRequest {

    @JsonProperty("refresh_token")
    private String refreshToken;
}
