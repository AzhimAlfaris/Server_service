package com.trs.application_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.application_service.dto.SensorQueryRequest;
import com.trs.application_service.dto.SensorQueryResponse;
import com.trs.application_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorDataClientService {

    private final RabbitTemplate rabbitTemplate;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public SensorQueryResponse getLatestSensorData(String authorizationHeader) {
        String email = extractEmail(authorizationHeader);
        return requestSensorData(email, "LATEST", 1);
    }

    public SensorQueryResponse getSensorHistory(String authorizationHeader, int limit) {
        String email = extractEmail(authorizationHeader);
        return requestSensorData(email, "HISTORY", limit);
    }

    private SensorQueryResponse requestSensorData(String email, String requestType, int limit) {
        try {
            String normalizedEmail = email == null ? null : email.trim().toLowerCase();
            log.info("Sending sensor query request type={} email={} limit={}", requestType, normalizedEmail, limit);
            String requestJson = objectMapper.writeValueAsString(new SensorQueryRequest(normalizedEmail, requestType, limit));
            Object rawResponse = rabbitTemplate.convertSendAndReceive(exchangeName, routingKey, requestJson);

            if (rawResponse == null) {
                throw new ResourceNotFoundException("Tidak ada respons dari microcontroller-service");
            }

            SensorQueryResponse response = objectMapper.readValue(rawResponse.toString(), SensorQueryResponse.class);
            if (!"success".equalsIgnoreCase(response.status())) {
                throw new ResourceNotFoundException(response.message());
            }
            log.info("Received sensor query response type={} email={} status={}", requestType, normalizedEmail, response.status());
            return response;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to request sensor data type={} email={} limit={}", requestType, email, limit, exception);
            throw new RuntimeException("Gagal mengambil data sensor: " + exception.getMessage(), exception);
        }
    }

    private String extractEmail(String authorizationHeader) {
        String email = jwtService.extractEmailFromAuthorizationHeader(authorizationHeader);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token tidak valid");
        }
        return email;
    }
}
