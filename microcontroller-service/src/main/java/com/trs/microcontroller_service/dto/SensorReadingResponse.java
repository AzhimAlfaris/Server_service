package com.trs.microcontroller_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SensorReadingResponse(Long id,
                                    String address,
                                    String email,
                                    LocalDateTime createdAt,
                                    List<PotReadingResponse> pots) {
}
