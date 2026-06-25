package com.trs.microcontroller_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceSettingsPublicResponse(String email,
                                          @JsonProperty("soil_type") String soilType) {
}
