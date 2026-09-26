package com.org.travel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    private final AmadeusProperties amadeusProperties;

    public RestClientConfig(AmadeusProperties amadeusProperties) {
        this.amadeusProperties = amadeusProperties;
    }

    /** Defines the amadeus rest client bean. */
    @Bean("amadeusRestClient")
    public RestClient amadeusRestClient() {
        return RestClient.builder()
                .requestFactory(boundedTimeouts())
                .baseUrl(amadeusProperties.getBaseUrl())
                .build();
    }

    /** Bounded connect/read timeouts — the JDK client's default read timeout is infinite, so a hung upstream would pin the calling thread. */
    private static JdkClientHttpRequestFactory boundedTimeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
