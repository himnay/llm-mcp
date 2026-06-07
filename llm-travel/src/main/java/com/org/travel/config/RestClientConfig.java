package com.org.travel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private final AmadeusProperties amadeusProperties;

    public RestClientConfig(AmadeusProperties amadeusProperties) {
        this.amadeusProperties = amadeusProperties;
    }

    @Bean("amadeusRestClient")
    public RestClient amadeusRestClient() {
        return RestClient.builder()
                .baseUrl(amadeusProperties.getBaseUrl())
                .build();
    }
}
