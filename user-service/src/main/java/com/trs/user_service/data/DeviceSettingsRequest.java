package com.trs.user_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record DeviceSettingsRequest(
        @NotBlank String email,
        @NotBlank String address,
        @NotBlank @JsonProperty("soil_type") String soilType) {
}
