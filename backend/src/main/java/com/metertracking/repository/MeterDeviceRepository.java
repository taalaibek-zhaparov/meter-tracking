package com.metertracking.repository;

import com.metertracking.entity.MeterDevice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterDeviceRepository extends JpaRepository<MeterDevice, Long> {

    @Query("SELECT m FROM MeterDevice m LEFT JOIN FETCH m.meterModel WHERE m.meterNumber = :meterNumber")
    Optional<MeterDevice> findByMeterNumber(@Param("meterNumber") String meterNumber);

    @Query("SELECT m FROM MeterDevice m WHERE LOWER(m.meterNumber) LIKE LOWER(CONCAT('%', :query, '%')) AND m.available = true")
    List<MeterDevice> searchByMeterNumber(@Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM MeterDevice m LEFT JOIN FETCH m.meterModel")
    List<MeterDevice> findAllWithModel();
}
