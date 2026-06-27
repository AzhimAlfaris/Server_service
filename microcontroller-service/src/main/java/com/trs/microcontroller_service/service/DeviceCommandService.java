package com.trs.microcontroller_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.trs.microcontroller_service.dto.DeviceCommandMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceCommandService {

    private final ConcurrentMap<String, DeviceCommandMessage> latestCommands = new ConcurrentHashMap<>();
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void store(DeviceCommandMessage commandMessage) {
        if (commandMessage == null || commandMessage.address() == null) {
            return;
        }
        latestCommands.put(normalizeAddress(commandMessage.address()), commandMessage);
    }

    public DeviceCommandMessage getLatest(String address) {
        String normalizedAddress = normalizeAddress(address);
        String queueName = buildQueueName(normalizedAddress);

        Object rawPayload = rabbitTemplate.receiveAndConvert(queueName);
        if (rawPayload != null) {
            try {
                DeviceCommandMessage commandMessage = objectMapper.readValue(rawPayload.toString(), DeviceCommandMessage.class);
                store(commandMessage);
            } catch (Exception exception) {
                throw new RuntimeException("Gagal membaca command untuk address " + normalizedAddress, exception);
            }
        }

        return latestCommands.get(normalizedAddress);
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase(Locale.ROOT);
    }

    private String buildQueueName(String address) {
        return "device.command." + address.replace(':', '.');
    }
}
