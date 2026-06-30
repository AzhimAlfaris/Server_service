package com.trs.security_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
        try {
            return userServicWebClient.post()
                    .uri("/api/users")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 409,
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new ResponseStatusException(HttpStatus.CONFLICT, body)))
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (WebClientRequestException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "User service tidak dapat dijangkau");
        }
    }

    // /api/users/{email}
    public UserResponse getUserByEmail(String email) {
        try {
            return userServicWebClient.get()
                    .uri("/api/users/{email}", email)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> response.bodyToMono(String.class).map(
                                    body -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email)))
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (WebClientRequestException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "User service tidak dapat dijangkau");
        }
    }

    public boolean emailExists(String email) {
        try {
            Boolean response = userServicWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/exists")
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return Boolean.TRUE.equals(response);
        } catch (WebClientRequestException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "User service tidak dapat dijangkau");
        } catch (WebClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.valueOf(exception.getStatusCode().value()),
                    exception.getResponseBodyAsString());
        }
    }

    public UserResponse updatePassword(UserRequest request) {
        try {
            return userServicWebClient.put()
                    .uri("/api/users/update-password")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            response -> response.bodyToMono(String.class).map(
                                    body -> new ResponseStatusException(HttpStatus.NOT_FOUND, body)))
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (WebClientRequestException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "User service tidak dapat dijangkau");
        }
    }

}
