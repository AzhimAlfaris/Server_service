package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeviceSettingsItemResponse(String address,
                                         @JsonProperty("soil_types") List<String> soilTypes) {
}
