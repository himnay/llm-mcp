package com.org.github.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubClientConfig {

    /** Defines the cache manager bean. */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, GitHubProperties props) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(props.getCacheTtlSeconds()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /** Defines the git hub rest client bean. */
    @Bean
    public RestClient gitHubRestClient(GitHubProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(boundedTimeouts())
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (props.getToken() != null && !props.getToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.getToken());
        }
        return builder.build();
    }

    /** Handles warn if no token. */
    @EventListener(ContextRefreshedEvent.class)
    public void warnIfNoToken(ContextRefreshedEvent event) {
        GitHubProperties props = event.getApplicationContext().getBean(GitHubProperties.class);
        if (props.getToken() == null || props.getToken().isBlank()) {
            log.warn("GitHub token not configured – API calls will be unauthenticated (lower rate limits)");
        }
    }

    /** Bounded connect/read timeouts — the JDK client's default read timeout is infinite, so a hung upstream would pin the calling thread. */
    private static JdkClientHttpRequestFactory boundedTimeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
