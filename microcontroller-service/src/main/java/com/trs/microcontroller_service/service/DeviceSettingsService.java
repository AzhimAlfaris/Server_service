package com.trs.microcontroller_service.service;

import com.trs.microcontroller_service.client.UserServiceClient;
import com.trs.microcontroller_service.dto.DeviceSettingsPublicResponse;
import com.trs.microcontroller_service.dto.DeviceSettingsResponse;
import com.trs.microcontroller_service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceSettingsService {

    private final UserServiceClient userServiceClient;

    public DeviceSettingsPublicResponse getByAddress(String address) {
        DeviceSettingsResponse response = userServiceClient.getDeviceSettingsByAddress(address);
        if (response == null) {
            throw new ResourceNotFoundException("Device settings tidak ditemukan untuk address: " + normalizeAddress(address));
        }

        return new DeviceSettingsPublicResponse(response.email(), response.soilType());
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase();
    }
}
