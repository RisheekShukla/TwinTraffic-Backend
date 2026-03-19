package com.shadowtool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ShadowPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShadowPlatformApplication.class, args);
    }
}
