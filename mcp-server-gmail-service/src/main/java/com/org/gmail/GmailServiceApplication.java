package com.org.gmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
class GmailServiceApplication {

    /** Application entry point. */
    public static void main(String[] args) {
        SpringApplication.run(GmailServiceApplication.class, args);
    }
}
