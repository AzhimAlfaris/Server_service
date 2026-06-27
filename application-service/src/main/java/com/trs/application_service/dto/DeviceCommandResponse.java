package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceCommandResponse(String status,
                                    String message,
                                    String email,
                                    String address,
                                    String routingKey) {
}
