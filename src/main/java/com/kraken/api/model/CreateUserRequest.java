package com.kraken.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;

@Data
public class CreateUserRequest {

    public CreateUserRequest(@NonNull DiscordUser user, @NonNull String hardwareId) {
        this.discordEmail = user.getEmail();
        this.discordId = user.getId();
        this.discordUsername = user.getUsername();
        this.hardwareId = hardwareId;
    }

    @JsonProperty("hardware_id")
    private String hardwareId;

    @JsonProperty("discord_id")
    private String discordId;

    @JsonProperty("discord_email")
    private String discordEmail;

    @JsonProperty("discord_username")
    private String discordUsername;
}
