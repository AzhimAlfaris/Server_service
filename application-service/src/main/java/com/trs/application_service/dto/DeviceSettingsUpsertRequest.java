package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceSettingsUpsertRequest(String email,
                                          String address,
                                          @JsonProperty("soil_type") String soilType) {
}
