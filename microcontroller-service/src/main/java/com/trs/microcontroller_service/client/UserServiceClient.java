package com.trs.microcontroller_service.client;

import com.trs.microcontroller_service.dto.DeviceSettingsResponse;
import com.trs.microcontroller_service.exception.ResourceNotFoundException;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userServiceWebClient;

    public DeviceSettingsResponse getDeviceSettingsByAddress(String address) {
        String normalizedAddress = normalizeAddress(address);
        return userServiceWebClient.get()
                .uri("/api/users/device-settings/{address}", normalizedAddress)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new ResourceNotFoundException(
                                        "Device settings tidak ditemukan untuk address: " + normalizedAddress)))
                .bodyToMono(DeviceSettingsResponse.class)
                .block();
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase();
    }
}
