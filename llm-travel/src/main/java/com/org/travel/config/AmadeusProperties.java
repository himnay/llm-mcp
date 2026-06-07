package com.org.travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "amadeus")
public class AmadeusProperties {

    private String baseUrl = "https://test.api.amadeus.com";
    private String clientId = "";
    private String clientSecret = "";
    /** Connect + read timeout in seconds for Amadeus HTTP calls. */
    private int timeoutSeconds = 10;
}
