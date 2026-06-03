package com.metertracking.repository;

import com.metertracking.entity.MeterModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface MeterModelRepository extends JpaRepository<MeterModel, Long> {

    boolean existsByCode(String code);

    Optional<MeterModel> findByCode(String code);

    /**
     * Batch-загрузка моделей по набору кодов — используется при импорте из 1С
     * чтобы не делать N отдельных SELECT-ов.
     */
    @Query("SELECT m FROM MeterModel m WHERE m.code IN :codes")
    Set<MeterModel> findAllByCodes(@Param("codes") Set<String> codes);

    /**
     * Поиск по названию модели (для подсказки при регистрации счётчика).
     */
    @Query("SELECT m FROM MeterModel m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<MeterModel> searchByName(@Param("query") String query, Pageable pageable);
}