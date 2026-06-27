package com.trs.user_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeviceSettingsResponse(Long id,
                                     String email,
                                     String address,
                                     @JsonProperty("soil_types") List<String> soilTypes) {
}
