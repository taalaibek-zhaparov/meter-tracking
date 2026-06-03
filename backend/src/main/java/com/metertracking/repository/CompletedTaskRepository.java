package com.metertracking.repository;

import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий выполненных задач.
 *
 * Важно: все методы используют LEFT JOIN FETCH для загрузки связанных
 * сущностей (user, plan) в ОДНОМ запросе.
 *
 * Без JOIN FETCH Hibernate делал бы N+1 запросов:
 * - 1 запрос SELECT * FROM completed_task_data
 * - N запросов SELECT * FROM user_data WHERE id=? (для каждой задачи)
 * - N запросов SELECT * FROM plan_data WHERE id=? (для каждой задачи)
 *
 * С JOIN FETCH: 1 запрос с LEFT JOIN — в 10-100 раз быстрее.
 *
 * ВАЖНО: метод называется findAllWithRelations(), а не findAll()!
 * Переопределение JpaRepository.findAll() через @Query вызывает
 * конфликт с транзакционным прокси Spring Data и может приводить
 * к LazyInitializationException в некоторых версиях Hibernate.
 */
@Repository
public interface CompletedTaskRepository extends JpaRepository<CompletedTask, Long> {

    /**
     * Загружает все задачи с пользователями и планами за 1 SQL запрос.
     * Используется в AdminController и UserController.
     */
    @Query("SELECT t FROM CompletedTask t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.plan ORDER BY t.id DESC")
    List<CompletedTask> findAllWithRelations();

    /**
     * Загружает задачи конкретного пользователя с JOIN FETCH.
     * Используется в UserController для страницы "Выполненные задачи".
     */
    @Query("SELECT t FROM CompletedTask t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.plan WHERE t.user = :user")
    List<CompletedTask> findByUserWithRelations(@Param("user") User user);

    /**
     * Загружает конкретную задачу по ID с JOIN FETCH.
     * Используется при редактировании и просмотре истории.
     */
    @Query("SELECT t FROM CompletedTask t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.plan WHERE t.id = :id")
    Optional<CompletedTask> findByIdWithRelations(@Param("id") Long id);


    /** Выполненные задачи конкретного РЭС — для AdminRes контроллера */
    @Query("SELECT t FROM CompletedTask t LEFT JOIN FETCH t.user LEFT JOIN FETCH t.plan WHERE t.region = :region ORDER BY t.id DESC")
    List<CompletedTask> findAllByRegion(@Param("region") String region);

    /* Бэкенд: генерировать номер при сохранении в CompletedTaskService.java*/
    @Query("SELECT COUNT(c) FROM CompletedTask c")
    Long countAll();
}