package com.trs.application_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceCommandMessage(String email,
                                   String address,
                                   String command,
                                   @JsonProperty("duration") Integer duration,
                                   @JsonProperty("pot_index") Integer potIndex) {
}
