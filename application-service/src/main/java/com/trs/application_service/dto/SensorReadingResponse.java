package com.trs.application_service.dto;

import java.time.LocalDateTime;

public record SensorReadingResponse(Long id,
                                    String email,
                                    String microcontrollerId,
                                    String sensorValue,
                                    String moisturePercent,
                                    String soilCondition,
                                    String action,
                                    String pumpDuration,
                                    String timestampSensor,
                                    LocalDateTime createdAt) {
}
