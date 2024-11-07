package com.kraken.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateUserRequest {

    @JsonProperty("discord_id")
    private String discordId;

    @JsonProperty("discord_email")
    private String discordEmail;

    @JsonProperty("discord_username")
    private String discordUsername;
}
