package com.trs.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "device_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_settings_address", columnNames = "address"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String address;

    @Column(name = "soil_type", nullable = false)
    private String soilType;

}
