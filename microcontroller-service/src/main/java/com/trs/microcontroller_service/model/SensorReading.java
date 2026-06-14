package com.trs.microcontroller_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "microcontroller_id", nullable = false, length = 100)
    private String microcontrollerId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "sensor_value", nullable = false, length = 255)
    private String sensorValue;

    @Column(name = "moisture_percent", nullable = false, length = 255)
    private String moisturePercent;

    @Column(name = "soil_condition", nullable = false, length = 255)
    private String soilCondition;

    @Column(name = "action", nullable = false, length = 255)
    private String action;

    @Column(name = "pump_duration", nullable = false, length = 255)
    private String pumpDuration;

    @Column(name = "timestamp_sensor", nullable = false, length = 255)
    private String timestampSensor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SensorReading create(String microcontrollerId, String sensorValue, String moisturePercent,
                                       String soilCondition, String action, String pumpDuration, String email,
                                       String timestampSensor) {
        SensorReading sensorReading = new SensorReading();
        sensorReading.microcontrollerId = microcontrollerId;
        sensorReading.email = email;
        sensorReading.sensorValue = sensorValue;
        sensorReading.moisturePercent = moisturePercent;
        sensorReading.soilCondition = soilCondition;
        sensorReading.action = action;
        sensorReading.pumpDuration = pumpDuration;
        sensorReading.timestampSensor = timestampSensor;
        return sensorReading;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
