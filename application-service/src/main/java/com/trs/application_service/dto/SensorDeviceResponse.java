package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SensorDeviceResponse(String address,
                                   List<PotReadingResponse> pots) {
}
