package com.trs.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "device_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_settings_address", columnNames = "address"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSetting {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String address;

    @Column(name = "soil_type", nullable = false)
    private String soilType;

    @Column(name = "soil_types", columnDefinition = "TEXT")
    private String soilTypesJson;

    @Transient
    public List<String> getSoilTypes() {
        if (soilTypesJson != null && !soilTypesJson.isBlank()) {
            try {
                return OBJECT_MAPPER.readValue(soilTypesJson, LIST_OF_STRING);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Gagal membaca soil types untuk address " + address, exception);
            }
        }

        if (soilType == null || soilType.isBlank()) {
            return List.of();
        }

        return List.of(soilType);
    }

    public void setSoilTypes(List<String> soilTypes) {
        List<String> safeSoilTypes = soilTypes == null ? List.of() : new ArrayList<>(soilTypes);
        this.soilTypesJson = serializeSoilTypes(safeSoilTypes);
        this.soilType = safeSoilTypes.isEmpty() ? null : safeSoilTypes.get(0);
    }

    private String serializeSoilTypes(List<String> soilTypes) {
        try {
            return OBJECT_MAPPER.writeValueAsString(soilTypes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gagal menyimpan soil types untuk address " + address, exception);
        }
    }

}
