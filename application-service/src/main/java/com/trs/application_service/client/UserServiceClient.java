package com.trs.application_service.client;

import com.trs.application_service.dto.DeviceSettingsResponse;
import com.trs.application_service.dto.DeviceSettingsUpsertRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userServiceWebClient;

    public DeviceSettingsResponse upsertDeviceSettings(DeviceSettingsUpsertRequest request) {
        return userServiceWebClient.put()
                .uri("/api/users/device-settings")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new ResponseStatusException(HttpStatus.CONFLICT, body)))
                .onStatus(status -> status.value() == 404,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new ResponseStatusException(HttpStatus.NOT_FOUND, body)))
                .bodyToMono(DeviceSettingsResponse.class)
                .block();
    }

}
