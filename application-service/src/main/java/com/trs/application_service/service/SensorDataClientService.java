package com.trs.application_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.application_service.client.UserServiceClient;
import com.trs.application_service.dto.DeviceCommandMessage;
import com.trs.application_service.dto.DeviceCommandResponse;
import com.trs.application_service.dto.DeviceCommandRequest;
import com.trs.application_service.dto.DeviceSettingsRequest;
import com.trs.application_service.dto.DeviceSettingsItemResponse;
import com.trs.application_service.dto.DeviceSettingsQueryResponse;
import com.trs.application_service.dto.DeviceSettingsResponse;
import com.trs.application_service.dto.DeviceSettingsUpsertRequest;
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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorDataClientService {

    private final RabbitTemplate rabbitTemplate;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;
    private final DeviceCommandPublisherService deviceCommandPublisherService;
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

    public DeviceSettingsQueryResponse getDeviceSettingsForDashboard(String authorizationHeader) {
        String email = extractEmail(authorizationHeader);
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        List<DeviceSettingsResponse> deviceSettings = userServiceClient.getDeviceSettingsByEmail(normalizedEmail);

        List<DeviceSettingsItemResponse> devices = deviceSettings == null ? List.of() : deviceSettings.stream()
                .map(device -> new DeviceSettingsItemResponse(
                        device.address(),
                        device.soilType()))
                .collect(Collectors.toList());

        return new DeviceSettingsQueryResponse(
                "success",
                "Daftar device settings berhasil diambil",
                normalizedEmail,
                devices);
    }

    public DeviceSettingsResponse saveOrUpdateDeviceSettings(String authorizationHeader, DeviceSettingsRequest request) {
        String email = extractEmail(authorizationHeader);
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedAddress = normalizeAddress(request.address());
        String normalizedSoilType = request.soilType() == null ? null : request.soilType().trim();

        log.info("Forwarding device settings update email={} address={}", normalizedEmail, normalizedAddress);
        DeviceSettingsResponse response = userServiceClient.upsertDeviceSettings(new DeviceSettingsUpsertRequest(
                normalizedEmail,
                normalizedAddress,
                normalizedSoilType));
        deviceCommandPublisherService.ensureQueue(normalizedAddress);
        return response;
    }

    public DeviceCommandResponse sendManualWateringCommand(String authorizationHeader, DeviceCommandRequest request) {
        String requesterEmail = extractEmail(authorizationHeader);
        String normalizedRequesterEmail = requesterEmail == null ? null : requesterEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedAddress = normalizeAddress(request.address());
        String normalizedCommand = normalizeCommand(request.command());
        Integer duration = request.duration();

        DeviceSettingsResponse deviceSettings;
        try {
            deviceSettings = userServiceClient.getDeviceSettingsByAddress(normalizedAddress);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Address " + normalizedAddress + " tidak terdaftar di device settings");
            }
            throw exception;
        }

        String registeredEmail = normalizeEmail(deviceSettings.email());
        if (!registeredEmail.equals(normalizedRequesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email " + normalizedRequesterEmail + " tidak memiliki address " + normalizedAddress);
        }

        DeviceCommandMessage commandMessage = new DeviceCommandMessage(
                normalizedRequesterEmail,
                normalizedAddress,
                normalizedCommand,
                duration);

        try {
            String queueName = deviceCommandPublisherService.publish(commandMessage);
            log.info("Published manual command address={} queue={} duration={}",
                    normalizedAddress, queueName, duration);
            return new DeviceCommandResponse(
                    "success",
                    "Perintah manual watering berhasil dikirim",
                    normalizedRequesterEmail,
                    normalizedAddress,
                    queueName);
        } catch (Exception exception) {
            log.error("Failed to publish manual command address={}", normalizedAddress, exception);
            throw new RuntimeException("Gagal mengirim perintah manual: " + exception.getMessage(), exception);
        }
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

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCommand(String command) {
        return command == null ? null : command.trim().toUpperCase(Locale.ROOT);
    }

}
