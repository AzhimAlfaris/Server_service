package com.trs.application_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceCommandRequest(
        @NotBlank String address,
        @NotBlank String command,
        @NotNull @Min(1) Integer duration) {
}
