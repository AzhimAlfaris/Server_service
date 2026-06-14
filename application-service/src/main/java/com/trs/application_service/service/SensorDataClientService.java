package com.trs.application_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.application_service.dto.SensorQueryRequest;
import com.trs.application_service.dto.SensorQueryResponse;
import com.trs.application_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorDataClientService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public SensorQueryResponse getLatestSensorData(String microcontrollerId) {
        return requestSensorData(microcontrollerId, "LATEST", 1);
    }

    public SensorQueryResponse getSensorHistory(String microcontrollerId, int limit) {
        return requestSensorData(microcontrollerId, "HISTORY", limit);
    }

    private SensorQueryResponse requestSensorData(String microcontrollerId, String requestType, int limit) {
        try {
            String requestJson = objectMapper.writeValueAsString(new SensorQueryRequest(microcontrollerId, requestType, limit));
            Object rawResponse = rabbitTemplate.convertSendAndReceive(exchangeName, routingKey, requestJson);

            if (rawResponse == null) {
                throw new ResourceNotFoundException("Tidak ada respons dari microcontroller-service");
            }

            SensorQueryResponse response = objectMapper.readValue(rawResponse.toString(), SensorQueryResponse.class);
            if (!"success".equalsIgnoreCase(response.status())) {
                throw new ResourceNotFoundException(response.message());
            }
            return response;
        } catch (Exception exception) {
            throw new RuntimeException("Gagal mengambil data sensor: " + exception.getMessage(), exception);
        }
    }
}
