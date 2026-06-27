package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeviceSettingsUpsertRequest(String email,
                                          String address,
                                          @JsonProperty("soil_types") List<String> soilTypes) {
}
