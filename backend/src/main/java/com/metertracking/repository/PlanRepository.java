package com.metertracking.repository;

import com.metertracking.entity.Plan;
import com.metertracking.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий планов замены счётчиков.
 *
 * Все методы чтения используют LEFT JOIN FETCH p.user чтобы избежать
 * N+1 проблемы: без FETCH каждый план триггерил бы отдельный SELECT к user_data.
 *
 * Пример N+1 без JOIN FETCH (было):
 *   SELECT * FROM plan_data                           -- 1 запрос
 *   SELECT * FROM user_data WHERE id=1               -- запрос 1
 *   SELECT * FROM user_data WHERE id=1               -- запрос 2 (тот же user!)
 *   SELECT * FROM user_data WHERE id=2               -- запрос 3
 *   ... x количество планов
 *
 * С JOIN FETCH (стало):
 *   SELECT p.*, u.* FROM plan_data p LEFT JOIN user_data u ON u.id=p.user_id
 *   -- 1 запрос для любого количества планов
 *
 * ВАЖНО: метод findAllWithRelations() вместо findAll() —
 * см. комментарий в CompletedTaskRepository.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Все планы с данными пользователей — для AdminController.
     * 1 SQL запрос вместо 1+N.
     */
    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.user ORDER BY p.id DESC")
    List<Plan> findAllWithRelations();

    /**
     * Активные (невыполненные) планы пользователя — для страницы "Мои задачи".
     * completed = false фильтрует только актуальные задачи.
     */
    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.user WHERE p.user = :user AND p.completed = false")
    List<Plan> findByUserAndCompletedFalse(@Param("user") User user);

    /**
     * Все планы пользователя включая выполненные — для истории и Excel экспорта.
     */
    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.user WHERE p.user = :user")
    List<Plan> findByUser(@Param("user") User user);

    /**
     * Загружает план с пессимистической блокировкой (SELECT FOR UPDATE).
     *
     * Зачем: при выполнении задачи двумя пользователями одновременно
     * без блокировки оба могут прочитать plan.completed = false,
     * оба пройдут проверку и создадут дублирующие CompletedTask.
     *
     * PESSIMISTIC_WRITE блокирует строку в PostgreSQL на время транзакции —
     * второй запрос ждёт пока первый не завершится (commit/rollback).
     * Это гарантирует что задача выполняется ровно один раз.
     */

    /** Все планы конкретного РЭС — для AdminRes контроллера */
    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.user WHERE p.region = :region ORDER BY p.id DESC")
    List<Plan> findAllByRegion(@Param("region") String region);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Plan p WHERE p.id = :id")
    Optional<Plan> findByIdForUpdate(@Param("id") Long id);
}