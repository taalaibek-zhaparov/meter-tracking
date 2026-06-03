package com.metertracking.repository;

import com.metertracking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий пользователей.
 *
 * ВАЖНО: User имеет LAZY коллекцию roles (@ManyToMany).
 * При open-in-view=false (наша настройка) сессия Hibernate закрывается
 * сразу после выхода из @Transactional метода.
 * Если Jackson попытается сериализовать user.roles после закрытия сессии —
 * LazyInitializationException → пустой ответ или 500 ошибка.
 *
 * Решение: все методы используют LEFT JOIN FETCH u.roles —
 * роли загружаются сразу в одном SQL запросе, LazyInit не происходит.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Загружает пользователя по email с ролями (JOIN FETCH).
     * Используется в CustomUserDetailsService, AuthService, UserController.
     * 1 SQL запрос вместо 2 (user + roles).
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Загружает ВСЕХ активных пользователей с ролями.
     * ИСПРАВЛЕНО: добавлен JOIN FETCH — без него roles = LazyInit ошибка
     * при open-in-view=false.
     * Используется в AdminController.getAllUsers().
     * DISTINCT нужен из-за JOIN: без него каждый user дублируется
     * по количеству своих ролей.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAll();

    /**
     * Проверяет существование пользователя по email.
     * Используется при регистрации для проверки дублей.
     */
    boolean existsByEmail(String email);

    /**
     * Загружает ВСЕХ пользователей включая удалённых (soft delete).
     * @Where(deleted=false) на сущности User обходится этим запросом.
     * Используется для административного просмотра удалённых аккаунтов.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllIncludingDeleted();

    /**
     * Загружает только удалённых пользователей.
     * Используется для восстановления аккаунтов.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.deleted = true")
    List<User> findAllDeleted();

    /**
     * Восстанавливает удалённого пользователя (soft delete reverse).
     * @Modifying нужен для UPDATE/DELETE запросов через @Query.
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = false, u.deletedAt = null WHERE u.id = :id")
    void restoreUser(@Param("id") Long id);
    @Modifying
    @Query(value = "DELETE FROM user_data WHERE id = :id", nativeQuery = true)
    void deleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_data WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM change_history WHERE user_id = :userId", nativeQuery = true)
    void deleteChangeHistoryByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM change_history WHERE completed_task_id IN (SELECT id FROM completed_task_data WHERE user_id = :userId)", nativeQuery = true)
    void deleteChangeHistoryByCompletedTaskUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM completed_task_data WHERE user_id = :userId", nativeQuery = true)
    void deleteCompletedTasksByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE plan_data SET completed = false WHERE user_id = :userId", nativeQuery = true)
    void resetPlansByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM plan_data WHERE user_id = :userId", nativeQuery = true)
    void deletePlansByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_role WHERE user_id = :userId", nativeQuery = true)
    void deleteUserRoles(@Param("userId") Long userId);

    /** Пользователи с ролью USER конкретного региона — для AdminRes */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles r " +
            "WHERE LOWER(u.region) = LOWER(:region) " +
            "AND u.deleted = false AND r.name = 'USER'")
    List<User> findByRegionAndNotDeleted(@Param("region") String region);

}