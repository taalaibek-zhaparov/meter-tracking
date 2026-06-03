package com.metertracking.controller;

import com.metertracking.dto.PlanRequest;
import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.Plan;
import com.metertracking.entity.User;
import com.metertracking.repository.UserRepository;
import com.metertracking.service.CompletedTaskService;
import com.metertracking.service.OneCHttpService;
import com.metertracking.dto.OneCMeterReadingDTO;
import com.metertracking.service.PlanService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для Администраторов РЭС (роль ADMIN_RES).
 *
 * Доступные операции:
 *  - Создать план (только для пользователей своего РЭС)
 *  - Просмотр всех планов своего РЭС
 *  - Просмотр выполненных задач своего РЭС
 *
 * Все данные фильтруются по region текущего пользователя — нет доступа к чужим РЭС.
 */
@RestController
@RequestMapping("/api/admin-res")
@PreAuthorize("hasAnyRole('ADMIN_RES', 'ADMIN')")
@RequiredArgsConstructor
public class AdminResController {

    private final PlanService            planService;
    private final CompletedTaskService   completedTaskService;
    private final UserRepository         userRepository;
    private final OneCHttpService         oneCHttpService;

    // ── Утилита: получить текущего пользователя и его регион ─────────

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));
    }

    private String currentRegion(Authentication auth) {
        return currentUser(auth).getRegion();
    }

    // ── Создать план ──────────────────────────────────────────────────

    /**
     * Создаёт план замены счётчика.
     * Регион в плане автоматически берётся из профиля текущего AdminRes.
     */
    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@RequestBody PlanRequest request,
                                        Authentication auth) {
        try {
            // Принудительно подставляем регион AdminRes — нельзя создать план чужого РЭС
            String region = currentRegion(auth);
            request.setRegion(region);

            Plan plan = planService.createPlan(request);
            return ResponseEntity.ok(Map.of("message", "План успешно создан!", "plan", plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Планы своего РЭС ─────────────────────────────────────────────

    /**
     * Все планы только своего региона.
     */
    @GetMapping("/plans")
    public ResponseEntity<List<Plan>> getMyRegionPlans(Authentication auth) {
        String region = currentRegion(auth);
        return ResponseEntity.ok(planService.getPlansByRegion(region));
    }

    /**
     * Удалить план (только своего РЭС).
     */
    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable Long planId,
                                        Authentication auth) {
        try {
            String region = currentRegion(auth);
            // Проверяем что план принадлежит нашему региону
            Plan plan = planService.getPlanById(planId);
            if (!region.equals(plan.getRegion())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Нет доступа к плану другого РЭС"));
            }
            planService.deletePlan(planId);
            return ResponseEntity.ok(Map.of("message", "План удалён!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Выполненные задачи своего РЭС ────────────────────────────────

    /**
     * Все выполненные задачи только своего региона.
     */
    @GetMapping("/completed-tasks")
    public ResponseEntity<List<CompletedTask>> getMyRegionCompletedTasks(Authentication auth) {
        String region = currentRegion(auth);
        return ResponseEntity.ok(completedTaskService.getCompletedTasksByRegion(region));
    }

    // ── Данные из 1С (для создания планов) ───────────────────────────

    /**
     * Получить данные счётчика из 1С по лицевому счёту.
     * Дублирует AdminController endpoint — доступен для ADMIN_RES.
     * GET /api/admin-res/onec/meter/{licevoy}
     */
    @GetMapping("/onec/meter/{licevoy}")
    public ResponseEntity<?> getMeterFromOnec(@PathVariable String licevoy) {
        Optional<OneCMeterReadingDTO> data = oneCHttpService.getLastMeterInfo(licevoy);
        return data.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Информация о себе ─────────────────────────────────────────────

    /**
     * Возвращает профиль текущего AdminRes (имя, регион).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(Map.of(
                "id",       user.getId(),
                "username", user.getUsername(),
                "email",    user.getEmail(),
                "region",   user.getRegion() != null ? user.getRegion() : ""
        ));
    }

    // ── Список пользователей своего РЭС ──────────────────────────────

    /**
     * Мастера только своего региона (для назначения планов).
     */
    @GetMapping("/users")
    public ResponseEntity<?> getMyRegionUsers(Authentication auth) {
        String region = currentRegion(auth);
        List<User> users = userRepository.findByRegionAndNotDeleted(region);
        return ResponseEntity.ok(users);
    }
}
