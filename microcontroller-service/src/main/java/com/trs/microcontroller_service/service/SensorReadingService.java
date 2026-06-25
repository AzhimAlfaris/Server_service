package com.trs.microcontroller_service.service;

import com.trs.microcontroller_service.dto.PotReadingRequest;
import com.trs.microcontroller_service.dto.PotReadingResponse;
import com.trs.microcontroller_service.dto.DeviceSettingsResponse;
import com.trs.microcontroller_service.dto.SensorDeviceResponse;
import com.trs.microcontroller_service.dto.SensorReadingRequest;
import com.trs.microcontroller_service.dto.SensorReadingResponse;
import com.trs.microcontroller_service.dto.SensorQueryResponse;
import com.trs.microcontroller_service.exception.ResourceNotFoundException;
import com.trs.microcontroller_service.model.PotDetail;
import com.trs.microcontroller_service.model.SensorReading;
import com.trs.microcontroller_service.repository.SensorReadingRepository;
import com.trs.microcontroller_service.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${app.rabbitmq.exchange:sensor.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification-routing-key:sensor.notification}")
    private String notificationRoutingKey;

    @Transactional
    public SensorReadingResponse save(SensorReadingRequest request) {
        String address = normalizeAddress(request.address());
        String email = normalizeEmail(request.email());

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address tidak boleh kosong");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email tidak boleh kosong");
        }
        if (request.pots() == null || request.pots().isEmpty()) {
            throw new IllegalArgumentException("Minimal satu pot harus dikirim");
        }

        DeviceSettingsResponse deviceSettings;
        try {
            deviceSettings = userServiceClient.getDeviceSettingsByAddress(address);
        } catch (ResourceNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Address " + address + " tidak terdaftar di device settings");
        }

        String registeredEmail = normalizeEmail(deviceSettings.email());
        if (!registeredEmail.equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email " + email + " tidak memiliki address " + address);
        }

        log.info("Saving sensor reading address={} pots={}", address, request.pots().size());

        SensorReading sensorReading = SensorReading.create(address, email);
        for (PotReadingRequest potRequest : request.pots()) {
            sensorReading.addPotDetail(PotDetail.create(
                    potRequest.potIndex(),
                    potRequest.sensorValue(),
                    potRequest.moisturePercent(),
                    potRequest.soilCondition(),
                    potRequest.action(),
                    potRequest.pumpDuration(),
                    potRequest.timestampSensor()));
        }

        SensorReading savedReading = sensorReadingRepository.save(sensorReading);
        SensorReadingResponse response = toResponse(savedReading);
        publishNotification(response);
        log.info("Sensor reading saved with id={} address={}", response.id(), response.address());
        return response;
    }

    @Transactional(readOnly = true)
    public SensorQueryResponse getLatestByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.info("Fetching latest sensor reading for email={}", normalizedEmail);
        List<String> addresses = sensorReadingRepository.findDistinctAddressesByEmail(normalizedEmail);
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("Data sensor untuk email " + normalizedEmail + " tidak ditemukan");
        }

        List<SensorDeviceResponse> devices = addresses.stream()
                .map(address -> sensorReadingRepository
                        .findByEmailAndAddressOrderByCreatedAtDesc(normalizedEmail, address, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .map(this::toLatestDeviceResponse)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new ResourceNotFoundException("Data sensor untuk email " + normalizedEmail + " tidak ditemukan");
        }

        return new SensorQueryResponse(
                "success",
                "Data sensor terbaru dari semua perangkat berhasil diambil",
                normalizedEmail,
                devices);
    }

    @Transactional(readOnly = true)
    public SensorQueryResponse getHistoryByEmail(String email, int limit) {
        String normalizedEmail = normalizeEmail(email);
        int safeLimit = limit > 0 ? limit : 5;
        log.info("Fetching sensor reading history for email={} limit={}", normalizedEmail, safeLimit);
        List<String> addresses = sensorReadingRepository.findDistinctAddressesByEmail(normalizedEmail);
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("Riwayat sensor untuk email " + normalizedEmail + " tidak ditemukan");
        }

        List<SensorDeviceResponse> devices = addresses.stream()
                .map(address -> {
                    List<SensorReading> sensorReadings = sensorReadingRepository
                            .findByEmailAndAddressOrderByCreatedAtDesc(normalizedEmail, address);
                    if (sensorReadings.isEmpty()) {
                        return null;
                    }
                    List<PotReadingResponse> historyPots = buildHistoryPots(sensorReadings, safeLimit);
                    if (historyPots.isEmpty()) {
                        return null;
                    }
                    return new SensorDeviceResponse(address, historyPots);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new ResourceNotFoundException("Riwayat sensor untuk email " + normalizedEmail + " tidak ditemukan");
        }

        return new SensorQueryResponse(
                "success",
                "Riwayat data sensor dari semua perangkat berhasil diambil",
                normalizedEmail,
                devices);
    }

    private SensorReadingResponse toResponse(SensorReading sensorReading) {
        return new SensorReadingResponse(
                sensorReading.getId(),
                sensorReading.getAddress(),
                sensorReading.getEmail(),
                sensorReading.getCreatedAt(),
                sensorReading.getPotDetails().stream().map(this::toPotResponse).collect(Collectors.toList()));
    }

    private SensorDeviceResponse toLatestDeviceResponse(SensorReading sensorReading) {
        return new SensorDeviceResponse(
                sensorReading.getAddress(),
                sensorReading.getPotDetails().stream().map(this::toPotResponse).collect(Collectors.toList()));
    }

    private List<PotReadingResponse> buildHistoryPots(List<SensorReading> sensorReadings, int limitPerPot) {
        Map<Integer, List<PotReadingResponse>> groupedByPotIndex = new java.util.TreeMap<>();

        sensorReadings.stream()
                .sorted(Comparator.comparing(SensorReading::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .forEach(sensorReading -> sensorReading.getPotDetails().stream()
                        .sorted(Comparator.comparing(PotDetail::getPotIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                        .forEach(potDetail -> {
                            List<PotReadingResponse> history = groupedByPotIndex.computeIfAbsent(
                                    potDetail.getPotIndex(),
                                    key -> new ArrayList<>());
                            if (history.size() < limitPerPot) {
                                history.add(toHistoryPotResponse(potDetail));
                            }
                        }));

        return groupedByPotIndex.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private PotReadingResponse toPotResponse(PotDetail potDetail) {
        return new PotReadingResponse(
                potDetail.getPotIndex(),
                potDetail.getSensorValue(),
                potDetail.getMoisturePercent(),
                potDetail.getSoilCondition(),
                potDetail.getAction(),
                potDetail.getPumpDuration(),
                potDetail.getTimestampSensor());
    }

    private PotReadingResponse toHistoryPotResponse(PotDetail potDetail) {
        return new PotReadingResponse(
                potDetail.getPotIndex(),
                null,
                potDetail.getMoisturePercent(),
                potDetail.getSoilCondition(),
                null,
                null,
                null);
    }

    private void publishNotification(SensorReadingResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend(exchangeName, notificationRoutingKey, payload);
            log.info("Published sensor notification for readingId={}", response.id());
        } catch (Exception exception) {
            log.error("Gagal menyiapkan notifikasi email", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase();
    }
}
