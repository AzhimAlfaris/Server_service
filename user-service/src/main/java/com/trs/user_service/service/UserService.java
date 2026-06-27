package com.trs.user_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.trs.user_service.data.DeviceSettingsRequest;
import com.trs.user_service.data.DeviceSettingsResponse;
import com.trs.user_service.data.UserRequest;
import com.trs.user_service.model.DeviceSetting;
import com.trs.user_service.model.User;
import com.trs.user_service.repository.DeviceSettingRepository;
import com.trs.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DeviceSettingRepository deviceSettingRepository;

    public User creatUser(UserRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if(userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exist: " + normalizedEmail);
        }

        // Data
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(request.getPassword());

        return userRepository.save(user);
    }

    public User updatePassword(UserRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + normalizedEmail));

        user.setPassword(request.getPassword());
        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public Boolean userExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    public DeviceSettingsResponse saveOrUpdateDeviceSettings(DeviceSettingsRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedAddress = normalizeAddress(request.address());
        List<String> normalizedSoilTypes = normalizeSoilTypes(request.soilTypes());

        DeviceSetting deviceSetting = deviceSettingRepository.findByAddress(normalizedAddress)
                .orElseGet(() -> new DeviceSetting(null, normalizedEmail, normalizedAddress, null, null));

        if (deviceSetting.getId() != null && !deviceSetting.getEmail().equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Address " + normalizedAddress + " sudah terdaftar untuk email lain");
        }

        deviceSetting.setEmail(normalizedEmail);
        deviceSetting.setAddress(normalizedAddress);
        deviceSetting.setSoilTypes(normalizedSoilTypes);

        DeviceSetting saved = deviceSettingRepository.save(deviceSetting);
        return toResponse(saved);
    }

    public DeviceSettingsResponse getDeviceSettingsByAddress(String address) {
        String normalizedAddress = normalizeAddress(address);
        DeviceSetting deviceSetting = deviceSettingRepository.findByAddress(normalizedAddress)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Device settings not found for address: " + normalizedAddress));
        return toResponse(deviceSetting);
    }

    public List<DeviceSettingsResponse> getDeviceSettingsByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return deviceSettingRepository.findAllByEmail(normalizedEmail).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeAddress(String address) {
        return address == null ? null : address.trim().toUpperCase();
    }

    private String normalizeValue(String value) {
        return value == null ? null : value.trim();
    }

    private List<String> normalizeSoilTypes(List<String> soilTypes) {
        if (soilTypes == null || soilTypes.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "soil_types harus berisi tepat 3 item");
        }

        List<String> normalized = new ArrayList<>(soilTypes.size());
        for (int index = 0; index < soilTypes.size(); index++) {
            String soilType = normalizeValue(soilTypes.get(index));
            if (soilType == null || soilType.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "soil_types item ke-" + (index + 1) + " tidak boleh kosong");
            }
            normalized.add(soilType);
        }
        return normalized;
    }

    private DeviceSettingsResponse toResponse(DeviceSetting deviceSetting) {
        return new DeviceSettingsResponse(
                deviceSetting.getId(),
                deviceSetting.getEmail(),
                deviceSetting.getAddress(),
                deviceSetting.getSoilTypes());
    }

}
