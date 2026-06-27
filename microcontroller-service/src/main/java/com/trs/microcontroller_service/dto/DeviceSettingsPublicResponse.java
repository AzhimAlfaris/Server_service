package com.trs.microcontroller_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeviceSettingsPublicResponse(String email,
                                          @JsonProperty("soil_types") List<String> soilTypes) {
}
