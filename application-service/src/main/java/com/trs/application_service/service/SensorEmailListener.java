package com.trs.application_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.application_service.dto.SensorReadingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@RequiredArgsConstructor
public class SensorEmailListener {

    private final SensorEmailService sensorEmailService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @RabbitListener(queues = "${app.rabbitmq.notification-queue}")
    public void handleSensorNotification(String payload) {
        try {
            SensorReadingResponse reading = objectMapper.readValue(payload, SensorReadingResponse.class);
            sensorEmailService.sendSensorUpdate(reading);
        } catch (Exception exception) {
            log.error("Gagal mengirim email sensor", exception);
        }
    }
}
