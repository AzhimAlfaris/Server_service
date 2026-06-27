package com.trs.application_service.service;

import java.util.Locale;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.application_service.dto.DeviceCommandMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceCommandPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange commandExchange;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public String publish(DeviceCommandMessage commandMessage) {
        String routingKey = buildRoutingKey(commandMessage.address(), commandMessage.potIndex());

        try {
            String payload = objectMapper.writeValueAsString(commandMessage);
            rabbitTemplate.convertAndSend(commandExchange.getName(), routingKey, payload);
            log.info("Published MQTT-style manual command exchange={} routingKey={} duration={}",
                    commandExchange.getName(), routingKey, commandMessage.duration());
            return routingKey;
        } catch (Exception exception) {
            throw new RuntimeException("Gagal mengirim perintah manual: " + exception.getMessage(), exception);
        }
    }

    public String buildRoutingKey(String address, Integer potIndex) {
        return "device/command/" + normalizeAddress(address).replace(":", "") + "/pot/" + potIndex;
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase(Locale.ROOT);
    }
}
