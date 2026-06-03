package com.metertracking.repository;

import com.metertracking.entity.ChangeHistory;
import com.metertracking.entity.CompletedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {

    // ИСПРАВЛЕНО: JOIN FETCH — грузим user и completedTask за 1 запрос
    @Query("SELECT h FROM ChangeHistory h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.completedTask WHERE h.completedTask = :task ORDER BY h.changeTime DESC")
    List<ChangeHistory> findByCompletedTaskOrderByChangeTimeDesc(@Param("task") CompletedTask task);

    @Query("SELECT h FROM ChangeHistory h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.completedTask ORDER BY h.changeTime DESC")
    List<ChangeHistory> findAllByOrderByChangeTimeDesc();
}
