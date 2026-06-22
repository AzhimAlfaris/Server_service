package com.trs.microcontroller_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${userservice.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public boolean userExists(String email) {
        Boolean response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/exists")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(response);
    }
}
