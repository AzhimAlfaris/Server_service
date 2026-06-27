package com.trs.microcontroller_service.controller;

import com.trs.microcontroller_service.dto.DeviceCommandMessage;
import com.trs.microcontroller_service.service.DeviceCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-commands")
@Slf4j
@RequiredArgsConstructor
public class DeviceCommandController {

    private final DeviceCommandService deviceCommandService;

    @GetMapping("/{address}")
    public ResponseEntity<DeviceCommandMessage> getLatestCommand(@PathVariable String address) {
        log.info("Request latest device command for address={}", address);
        DeviceCommandMessage command = deviceCommandService.getLatest(address);
        if (command == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(command);
    }
}
