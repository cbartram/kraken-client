package com.kraken.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreSignedURL {

    @JsonProperty("URL")
    String url;

    @JsonProperty("Method")
    String method;

    @JsonProperty("SignedHeader")
    Map<String, List<String>> signedHeader;
}
