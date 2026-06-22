package com.trs.microcontroller_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SensorReadingRequest(@NotBlank String address,
                                   @NotBlank String email,
                                   @NotEmpty List<@Valid PotReadingRequest> pots) {
}
