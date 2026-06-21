package com.trs.microcontroller_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PotReadingResponse(Integer potIndex,
                                 String sensorValue,
                                 String moisturePercent,
                                 String soilCondition,
                                 String action,
                                 String pumpDuration,
                                 String timestampSensor) {
}
