package com.trs.user_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trs.user_service.model.DeviceSetting;

@Repository
public interface DeviceSettingRepository extends JpaRepository<DeviceSetting, Long> {

    Optional<DeviceSetting> findByAddress(String address);

    boolean existsByAddress(String address);

    List<DeviceSetting> findAllByEmail(String email);

}
