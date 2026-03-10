package com.nexacorp.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HRClient {

    private final RestTemplate restTemplate;
    @Value("${services.hr.base-url}")
    private String hrBaseUrl;

    public void applyLeave(String username, String date) {
        log.info("Applying leave for: " + username);
        String url = hrBaseUrl + "/hr/leave?username=" + username + "&date=" + date;
        restTemplate.postForObject(url, null, String.class);
    }

    public String findReplacement(String username, String date) {
        log.info("Finding replacement for: " + username);
        String url = hrBaseUrl +
                "/hr/replacement/" + username +
                "?date=" + date;
        return restTemplate.getForObject(url, String.class);
    }

}
