package com.trs.microcontroller_service.dto;

public record SensorQueryRequest(String email, String requestType, Integer limit) {
}
