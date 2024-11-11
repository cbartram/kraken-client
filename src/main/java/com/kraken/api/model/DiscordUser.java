package com.kraken.api.model;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscordUser {
    private String discriminator;
    private String id;
    private String username;
    private String email;
    private String avatar;
    private boolean verified;
    private int flags;
    private String banner;

    @JsonAlias("public_flags")
    private int publicFlags;

    @JsonAlias("accent_color")
    private int accentColor;

    @JsonAlias("premium_type")
    private int premiumType;

    @JsonAlias("avatar_decoration_data")
    private Map<String, String> avatarDecorationData;
}
