package com.kraken.api.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscordOAuthRequest {
    private String code;
}
