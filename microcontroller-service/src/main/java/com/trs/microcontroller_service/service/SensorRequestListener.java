package com.trs.microcontroller_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.microcontroller_service.dto.SensorQueryRequest;
import com.trs.microcontroller_service.dto.SensorQueryResponse;
import com.trs.microcontroller_service.dto.SensorReadingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

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
                List<SensorReadingResponse> readings =
                        sensorReadingService.getHistoryByMicrocontrollerId(request.microcontrollerId(),
                                request.limit() == null ? 10 : request.limit());
                return objectMapper.writeValueAsString(new SensorQueryResponse(
                        "success",
                        "Riwayat data sensor berhasil diambil",
                        request.microcontrollerId(),
                        readings));
            }

            SensorReadingResponse latest = sensorReadingService.getLatestByMicrocontrollerId(request.microcontrollerId());
            return objectMapper.writeValueAsString(new SensorQueryResponse(
                    "success",
                    "Data sensor terbaru berhasil diambil",
                    request.microcontrollerId(),
                    List.of(latest)));
        } catch (Exception exception) {
            try {
                SensorQueryRequest request = objectMapper.readValue(payload, SensorQueryRequest.class);
                return objectMapper.writeValueAsString(new SensorQueryResponse(
                        "error",
                        exception.getMessage(),
                        request.microcontrollerId(),
                        List.of()));
            } catch (Exception ignored) {
                return "{\"status\":\"error\",\"message\":\"Gagal memproses request sensor\",\"microcontrollerId\":null,\"readings\":[]}";
            }
        }
    }
}
