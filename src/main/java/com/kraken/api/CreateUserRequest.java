package com.kraken.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kraken.auth.DiscordUser;
import lombok.Data;
import lombok.NonNull;

@Data
public class CreateUserRequest {

    public CreateUserRequest(@NonNull DiscordUser user) {
        this.discordEmail = user.getEmail();
        this.discordId = user.getId();
        this.discordUsername = user.getUsername();
    }

    @JsonProperty("discord_id")
    private String discordId;

    @JsonProperty("discord_email")
    private String discordEmail;

    @JsonProperty("discord_username")
    private String discordUsername;
}
