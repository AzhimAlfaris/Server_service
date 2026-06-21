package com.trs.microcontroller_service.dto;

import java.util.List;

public record SensorReadingRequest(String address,
                                   String email,
                                   List<PotReadingRequest> pots) {
}
