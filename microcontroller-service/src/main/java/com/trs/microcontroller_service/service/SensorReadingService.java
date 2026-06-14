package com.trs.microcontroller_service.service;

import com.trs.microcontroller_service.dto.SensorReadingRequest;
import com.trs.microcontroller_service.dto.SensorReadingResponse;
import com.trs.microcontroller_service.exception.ResourceNotFoundException;
import com.trs.microcontroller_service.model.SensorReading;
import com.trs.microcontroller_service.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${app.rabbitmq.exchange:sensor.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification-routing-key:sensor.notification}")
    private String notificationRoutingKey;

    public SensorReadingResponse save(SensorReadingRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email tidak boleh kosong");
        }

        SensorReading sensorReading = SensorReading.create(
                request.microcontrollerId(),
                request.sensorValue(),
                request.moisturePercent(),
                request.soilCondition(),
                request.action(),
                request.pumpDuration(),
                request.email().trim().toLowerCase(),
                request.timestampSensor());
        SensorReading savedReading = sensorReadingRepository.save(sensorReading);
        SensorReadingResponse response = toResponse(savedReading);
        publishNotification(response);
        return response;
    }

    public SensorReadingResponse getLatestByMicrocontrollerId(String microcontrollerId) {
        return sensorReadingRepository.findTopByMicrocontrollerIdOrderByCreatedAtDesc(microcontrollerId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Data sensor untuk microcontroller_id " + microcontrollerId + " tidak ditemukan"));
    }

    public List<SensorReadingResponse> getHistoryByMicrocontrollerId(String microcontrollerId, int limit) {
        List<SensorReading> sensorReadings = sensorReadingRepository.findByMicrocontrollerIdOrderByCreatedAtDesc(microcontrollerId);
        if (limit > 0 && limit < sensorReadings.size()) {
            sensorReadings = sensorReadings.subList(0, limit);
        }

        if (sensorReadings.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Riwayat sensor untuk microcontroller_id " + microcontrollerId + " tidak ditemukan");
        }

        return sensorReadings.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SensorReadingResponse toResponse(SensorReading sensorReading) {
        return new SensorReadingResponse(
                sensorReading.getId(),
                sensorReading.getEmail(),
                sensorReading.getMicrocontrollerId(),
                sensorReading.getSensorValue(),
                sensorReading.getMoisturePercent(),
                sensorReading.getSoilCondition(),
                sensorReading.getAction(),
                sensorReading.getPumpDuration(),
                sensorReading.getTimestampSensor(),
                sensorReading.getCreatedAt());
    }

    private void publishNotification(SensorReadingResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend(exchangeName, notificationRoutingKey, payload);
        } catch (Exception exception) {
            log.error("Gagal menyiapkan notifikasi email", exception);
        }
    }
}
