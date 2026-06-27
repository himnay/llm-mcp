package com.org.travel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "amadeus.client-id=test-id",
        "amadeus.client-secret=test-secret",
        "mcp.security.token=",
        "spring.ai.mcp.server.enabled=true"
})
class TravelServiceApplicationTests {

    @Test
    @DisplayName("Spring application context loads successfully")
    void contextLoads() {
    }
}
