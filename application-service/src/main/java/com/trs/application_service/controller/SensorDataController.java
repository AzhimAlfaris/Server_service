package com.trs.application_service.controller;

import com.trs.application_service.dto.DeviceSettingsRequest;
import com.trs.application_service.dto.DeviceSettingsQueryResponse;
import com.trs.application_service.dto.DeviceSettingsResponse;
import com.trs.application_service.dto.SensorQueryResponse;
import com.trs.application_service.service.SensorDataClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sensor-data")
@Slf4j
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataClientService sensorDataClientService;

    @GetMapping("/latest")
    public ResponseEntity<SensorQueryResponse> getLatest(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        log.info("Request latest sensor data with authorization header");
        return ResponseEntity.ok(sensorDataClientService.getLatestSensorData(authorizationHeader));
    }

    @GetMapping("/history")
    public ResponseEntity<SensorQueryResponse> getHistory(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                          @RequestParam(defaultValue = "5") int limit) {
        log.info("Request sensor history with authorization header limit={}", limit);
        return ResponseEntity.ok(sensorDataClientService.getSensorHistory(authorizationHeader, limit));
    }

    @GetMapping("/device-settings")
    public ResponseEntity<DeviceSettingsQueryResponse> getDeviceSettings(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        log.info("Request device settings list with authorization header");
        return ResponseEntity.ok(sensorDataClientService.getDeviceSettingsForDashboard(authorizationHeader));
    }

    @RequestMapping(value = "/device-settings", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<DeviceSettingsResponse> saveOrUpdateDeviceSettings(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody DeviceSettingsRequest request) {
        log.info("Request save/update device settings address={}", request.address());
        return ResponseEntity.ok(sensorDataClientService.saveOrUpdateDeviceSettings(authorizationHeader, request));
    }
}
