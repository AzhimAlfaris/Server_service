package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceSettingsQueryResponse(String status,
                                          String message,
                                          String email,
                                          List<DeviceSettingsItemResponse> devices) {
}
