package com.trs.application_service.controller;

import com.trs.application_service.dto.SensorQueryResponse;
import com.trs.application_service.service.SensorDataClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataClientService sensorDataClientService;

    @GetMapping("/latest/{microcontrollerId}")
    public ResponseEntity<SensorQueryResponse> getLatest(@PathVariable String microcontrollerId) {
        return ResponseEntity.ok(sensorDataClientService.getLatestSensorData(microcontrollerId));
    }

    @GetMapping("/history/{microcontrollerId}")
    public ResponseEntity<SensorQueryResponse> getHistory(@PathVariable String microcontrollerId,
                                                          @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(sensorDataClientService.getSensorHistory(microcontrollerId, limit));
    }
}
