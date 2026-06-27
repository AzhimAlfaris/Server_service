package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeviceSettingsRequest(
        @NotBlank String address,
        @NotEmpty @Size(min = 3, max = 3) @JsonProperty("soil_types") List<@NotBlank String> soilTypes) {
}
