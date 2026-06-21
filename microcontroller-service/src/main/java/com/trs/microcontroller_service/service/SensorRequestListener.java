package com.trs.microcontroller_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.microcontroller_service.dto.SensorQueryRequest;
import com.trs.microcontroller_service.dto.SensorQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorRequestListener {

    private final SensorReadingService sensorReadingService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public String handleSensorRequest(String payload) {
        try {
            SensorQueryRequest request = objectMapper.readValue(payload, SensorQueryRequest.class);
            String requestType = request.requestType() == null ? "LATEST" : request.requestType().trim().toUpperCase();

            if ("HISTORY".equals(requestType)) {
                return objectMapper.writeValueAsString(
                        sensorReadingService.getHistoryByEmail(request.email(), request.limit() == null ? 5 : request.limit()));
            }

            return objectMapper.writeValueAsString(sensorReadingService.getLatestByEmail(request.email()));
        } catch (Exception exception) {
            try {
                SensorQueryRequest request = objectMapper.readValue(payload, SensorQueryRequest.class);
                return objectMapper.writeValueAsString(new SensorQueryResponse(
                        "error",
                        exception.getMessage(),
                        request.email(),
                        java.util.List.of()));
            } catch (Exception ignored) {
                return "{\"status\":\"error\",\"message\":\"Gagal memproses request sensor\",\"email\":null,\"devices\":[]}";
            }
        }
    }
}
