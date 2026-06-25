package com.trs.user_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceSettingsResponse(Long id,
                                     String email,
                                     String address,
                                     @JsonProperty("soil_type") String soilType) {
}
