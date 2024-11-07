package com.kraken.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CognitoUser {
    private String cognitoId;
    private String discordId;
    private String discordUsername;
    private String email;
    private boolean accountEnabled;
    private CognitoCredentials credentials;
}
