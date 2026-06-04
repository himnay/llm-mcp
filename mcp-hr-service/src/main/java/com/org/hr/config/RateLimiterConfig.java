package com.org.hr.config;

import com.org.hr.security.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a single {@link RateLimiter} bean wired with the limit from
 * {@link SecurityProperties}.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter rateLimiter(SecurityProperties securityProperties) {
        return new RateLimiter(securityProperties.getRateLimitPerMinute());
    }
}
