package com.org.ai;

import com.org.ai.config.AssistantProperties;
import com.org.ai.mcp.KeycloakOAuth2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AssistantProperties.class, KeycloakOAuth2Properties.class})
public class AiAssistantMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantMcpApplication.class, args);
    }

}
