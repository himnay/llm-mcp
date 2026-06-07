package com.org.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelServiceApplication.class, args);
    }
}
