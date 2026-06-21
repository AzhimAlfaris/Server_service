package com.trs.application_service.dto;

public record PotReadingRequest(Integer potIndex,
                                String sensorValue,
                                String moisturePercent,
                                String soilCondition,
                                String action,
                                String pumpDuration,
                                String timestampSensor) {
}
