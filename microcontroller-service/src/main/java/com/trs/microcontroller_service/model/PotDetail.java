package com.trs.microcontroller_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pot_details")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PotDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_id", nullable = false)
    private SensorReading sensorReading;

    @Column(name = "pot_index", nullable = false)
    private Integer potIndex;

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

    public static PotDetail create(Integer potIndex, String sensorValue, String moisturePercent,
                                   String soilCondition, String action, String pumpDuration,
                                   String timestampSensor) {
        PotDetail potDetail = new PotDetail();
        potDetail.potIndex = potIndex;
        potDetail.sensorValue = sensorValue;
        potDetail.moisturePercent = moisturePercent;
        potDetail.soilCondition = soilCondition;
        potDetail.action = action;
        potDetail.pumpDuration = pumpDuration;
        potDetail.timestampSensor = timestampSensor;
        return potDetail;
    }
}
