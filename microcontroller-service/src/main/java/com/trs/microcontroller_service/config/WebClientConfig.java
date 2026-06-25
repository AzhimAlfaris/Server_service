package com.trs.microcontroller_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${userservice.base-url:http://localhost:8083}")
    private String userServiceBaseUrl;

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

}
