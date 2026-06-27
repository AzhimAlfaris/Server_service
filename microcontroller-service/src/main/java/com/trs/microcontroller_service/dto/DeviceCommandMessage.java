package com.trs.microcontroller_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceCommandMessage(String email,
                                   String address,
                                   String command,
                                   Integer duration) {
}
