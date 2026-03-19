package com.shadowtool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "v1WebClient")
    public WebClient v1WebClient(ShadowConfig shadowConfig) {
        return WebClient.builder()
                .baseUrl(shadowConfig.getV1BaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB max
                .build();
    }

    @Bean(name = "v2WebClient")
    public WebClient v2WebClient(ShadowConfig shadowConfig) {
        return WebClient.builder()
                .baseUrl(shadowConfig.getV2BaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Shadow-Request", "true")
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB max
                .build();
    }
}
