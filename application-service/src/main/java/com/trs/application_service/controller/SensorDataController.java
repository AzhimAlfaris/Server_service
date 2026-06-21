package com.trs.application_service.controller;

import com.trs.application_service.dto.SensorQueryResponse;
import com.trs.application_service.service.SensorDataClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-data")
@Slf4j
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataClientService sensorDataClientService;

    @GetMapping("/latest/{email}")
    public ResponseEntity<SensorQueryResponse> getLatest(@PathVariable String email) {
        log.info("Request latest sensor data for email={}", email);
        return ResponseEntity.ok(sensorDataClientService.getLatestSensorData(email));
    }

    @GetMapping("/history/{email}")
    public ResponseEntity<SensorQueryResponse> getHistory(@PathVariable String email,
                                                          @RequestParam(defaultValue = "5") int limit) {
        log.info("Request sensor history for email={} limit={}", email, limit);
        return ResponseEntity.ok(sensorDataClientService.getSensorHistory(email, limit));
    }
}
