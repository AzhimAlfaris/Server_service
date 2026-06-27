package com.trs.application_service.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange commandExchange;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void ensureQueue(String address) {
        String queueName = buildQueueName(address);
        Queue queue = buildDeviceQueue(queueName);
        Binding binding = BindingBuilder.bind(queue).to(commandExchange).with(queueName);

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
    }

    public String publish(DeviceCommandMessage commandMessage) {
        String queueName = buildQueueName(commandMessage.address());
        ensureQueue(commandMessage.address());

        try {
            String payload = objectMapper.writeValueAsString(commandMessage);
            rabbitTemplate.convertAndSend(commandExchange.getName(), queueName, payload);
            log.info("Published manual command queue={} routingKey={} duration={}",
                    queueName, queueName, commandMessage.duration());
            return queueName;
        } catch (Exception exception) {
            throw new RuntimeException("Gagal mengirim perintah manual: " + exception.getMessage(), exception);
        }
    }

    private Queue buildDeviceQueue(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-length", 1);
        args.put("x-overflow", "drop-head");
        return new Queue(queueName, true, false, false, args);
    }

    public String buildQueueName(String address) {
        return "device.command." + normalizeAddress(address).replace(':', '.');
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase(Locale.ROOT);
    }
}
