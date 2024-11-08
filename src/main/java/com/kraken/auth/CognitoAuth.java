package com.kraken.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CognitoAuth {
    String discordId;
    String refreshToken;
}
