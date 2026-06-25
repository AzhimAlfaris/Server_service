package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record DeviceSettingsRequest(
        @NotBlank String address,
        @NotBlank @JsonProperty("soil_type") String soilType) {
}
