package com.trs.microcontroller_service.repository;

import com.trs.microcontroller_service.model.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    @Query("select distinct s.address from SensorReading s where lower(s.email) = lower(:email) order by s.address asc")
    List<String> findDistinctAddressesByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = "potDetails")
    List<SensorReading> findByEmailAndAddressOrderByCreatedAtDesc(String email, String address, Pageable pageable);

    @EntityGraph(attributePaths = "potDetails")
    List<SensorReading> findByEmailAndAddressOrderByCreatedAtDesc(String email, String address);
}
