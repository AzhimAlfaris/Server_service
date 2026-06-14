package com.trs.microcontroller_service.dto;

public record SensorReadingRequest(String microcontrollerId,
                                  String email,
                                  String sensorValue,
                                  String moisturePercent,
                                  String soilCondition,
                                  String action,
                                  String pumpDuration,
                                  String timestampSensor) {
}
