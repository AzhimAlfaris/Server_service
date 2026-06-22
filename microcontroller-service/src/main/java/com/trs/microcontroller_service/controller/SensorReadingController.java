package com.trs.microcontroller_service.controller;

import com.trs.microcontroller_service.dto.SensorReadingRequest;
import com.trs.microcontroller_service.dto.SensorReadingResponse;
import com.trs.microcontroller_service.dto.SensorQueryResponse;
import com.trs.microcontroller_service.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-readings")
@Slf4j
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    public ResponseEntity<SensorReadingResponse> save(@Valid @RequestBody SensorReadingRequest request) {
        log.info("Request save sensor reading address={}", request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorReadingService.save(request));
    }

    @GetMapping("/latest/{email}")
    public ResponseEntity<SensorQueryResponse> getLatest(@PathVariable String email) {
        log.info("Request latest sensor reading for email={}", email);
        return ResponseEntity.ok(sensorReadingService.getLatestByEmail(email));
    }

    @GetMapping("/history/{email}")
    public ResponseEntity<SensorQueryResponse> getHistory(
            @PathVariable String email,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Request sensor reading history for email={} limit={}", email, limit);
        return ResponseEntity.ok(sensorReadingService.getHistoryByEmail(email, limit));
    }
}
