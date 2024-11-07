package com.kraken.api;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscordOAuthRequest {
    private String code;
}
