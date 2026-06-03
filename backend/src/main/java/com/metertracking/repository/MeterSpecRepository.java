package com.metertracking.repository;

import com.metertracking.entity.MeterSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface MeterSpecRepository extends JpaRepository<MeterSpec, Long> {

    boolean existsByMeterModelIdAndAmpAndDigitsAndPhaseAndVoltage(
            Long meterModelId, String amp, Short digits, Short phase, String voltage);

    /**
     * Batch-загрузка всех specs для набора моделей одним JOIN-запросом.
     * Используется при импорте для дедупликации без N+1.
     */
    @Query("""
        SELECT s FROM MeterSpec s
        JOIN FETCH s.meterModel
        WHERE s.meterModel.id IN :modelIds
        """)
    Set<MeterSpec> findAllByModelIds(@Param("modelIds") Set<Long> modelIds);
}