package com.kraken.api.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateLicenseRequest {
    CognitoCredentials credentials;
    String licenseKey;
    String hardwareId;
}
