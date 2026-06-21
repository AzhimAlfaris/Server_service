package com.trs.security_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.trs.security_service.data.UserRequest;
import com.trs.security_service.data.UserResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userServicWebClient;

    // /api/users
    public UserResponse createUser(UserRequest request) {
        return userServicWebClient.post()
                .uri("/api/users")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new ResponseStatusException(HttpStatus.CONFLICT, body)))
                .bodyToMono(UserResponse.class).block();
    }

    // /api/users/{email}
    public UserResponse getUserByEmail(String email) {
        return userServicWebClient.get()
                .uri("/api/users/{email}", email)
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        response -> response.bodyToMono(String.class).map(
                                body -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email)))
                .bodyToMono(UserResponse.class).block();
    }

}
