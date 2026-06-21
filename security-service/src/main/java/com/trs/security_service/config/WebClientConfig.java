package com.trs.security_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${userservice.base-url}")
    private String userServiceBaseUrl;

    @Bean
    public WebClient userServicWebClient() {
        return WebClient.builder()
            .baseUrl(userServiceBaseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

}
