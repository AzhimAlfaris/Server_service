package com.trs.microcontroller_service.controller;

import com.trs.microcontroller_service.dto.DeviceSettingsPublicResponse;
import com.trs.microcontroller_service.service.DeviceSettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-settings")
@Slf4j
@RequiredArgsConstructor
public class DeviceSettingsController {

    private final DeviceSettingsService deviceSettingsService;

    @GetMapping("/{address}")
    public ResponseEntity<DeviceSettingsPublicResponse> getByAddress(@PathVariable String address) {
        log.info("Request device settings for address={}", address);
        return ResponseEntity.ok(deviceSettingsService.getByAddress(address));
    }

}
