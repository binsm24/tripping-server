package com.tripping.trippingserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrippingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TrippingServerApplication.class,
                args
        );
    }
}
