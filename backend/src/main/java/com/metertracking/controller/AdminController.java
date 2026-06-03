package com.metertracking.controller;

import com.metertracking.dto.MeterDeviceDTO;
import com.metertracking.dto.OneCMeterReadingDTO;
import com.metertracking.dto.PlanRequest;
import com.metertracking.dto.RegisterRequest;
import com.metertracking.dto.UpdateCompletedTaskRequest;
import com.metertracking.entity.ChangeHistory;
import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.Plan;
import com.metertracking.entity.Role;
import com.metertracking.entity.User;
import com.metertracking.repository.RoleRepository;
import com.metertracking.repository.UserRepository;
import com.metertracking.service.CompletedTaskService;
import com.metertracking.service.ExcelExportService;
import com.metertracking.service.MeterDeviceService;
import com.metertracking.service.OneCHttpService;
import com.metertracking.service.PlanService;
import com.metertracking.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PlanService planService;
    private final CompletedTaskService completedTaskService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebSocketService webSocketService;
    private final OneCHttpService oneCHttpService; // ✅ HTTP вместо COM
    private final MeterDeviceService meterDeviceService;
    private final ExcelExportService excelExportService;

    // ─────────────────────────────────────────────────────
    // 1С ИНТЕГРАЦИЯ (HTTP)
    // ─────────────────────────────────────────────────────

    /**
     * Получить данные счётчика из 1С по лицевому счёту через HTTP.
     * Возвращает: region, tp, fio, adres, meterType, meterNumber, reading, date.
     */
    @GetMapping("/onec/meter/{licevoy}")
    public ResponseEntity<?> getMeterFromOnec(@PathVariable String licevoy) {
        Optional<OneCMeterReadingDTO> data = oneCHttpService.getLastMeterInfo(licevoy);
        return data.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Статус подключения к 1С HTTP публикации. */
    @GetMapping("/onec/status")
    public ResponseEntity<Map<String, Object>> getOnecStatus() {
        boolean available = oneCHttpService.isAvailable();
        Map<String, Object> result = new HashMap<>();
        result.put("available", available);
        result.put("connectionType", "HTTP");
        result.put("message", available ? "1С подключён (HTTP)" : "1С недоступен");
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────
    // БД СЧЁТЧИКОВ
    // ─────────────────────────────────────────────────────

    @GetMapping("/meter-devices")
    public ResponseEntity<List<MeterDeviceDTO>> getAllMeterDevices() {
        return ResponseEntity.ok(meterDeviceService.getAll());
    }

    @PostMapping("/meter-devices")
    public ResponseEntity<?> createMeterDevice(@RequestBody MeterDeviceDTO dto) {
        try {
            return ResponseEntity.ok(meterDeviceService.create(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/meter-devices/{id}")
    public ResponseEntity<?> updateMeterDevice(@PathVariable Long id, @RequestBody MeterDeviceDTO dto) {
        try {
            return ResponseEntity.ok(meterDeviceService.update(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/meter-devices/{id}")
    public ResponseEntity<?> deleteMeterDevice(@PathVariable Long id) {
        meterDeviceService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Счётчик удалён!"));
    }

    // ─────────────────────────────────────────────────────
    // ПЛАНЫ
    // ─────────────────────────────────────────────────────

    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@RequestBody PlanRequest request) {
        try {
            Plan plan = planService.createPlan(request);
            return ResponseEntity.ok(Map.of("message", "План успешно создан!", "plan", plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<?> updatePlan(@PathVariable Long planId, @RequestBody PlanRequest request) {
        try {
            Plan plan = planService.updatePlan(planId, request);
            return ResponseEntity.ok(Map.of("message", "План успешно обновлён!", "plan", plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable Long planId) {
        try {
            planService.deletePlan(planId);
            return ResponseEntity.ok(Map.of("message", "План успешно удалён!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/plans")
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    // ─────────────────────────────────────────────────────
    // ВЫПОЛНЕННЫЕ ЗАДАЧИ
    // ─────────────────────────────────────────────────────

    @GetMapping("/completed-tasks")
    public ResponseEntity<List<CompletedTask>> getAllCompletedTasks() {
        return ResponseEntity.ok(completedTaskService.getAllCompletedTasks());
    }

    @PutMapping("/completed-tasks/{taskId}")
    public ResponseEntity<?> updateCompletedTask(@PathVariable Long taskId,
                                                 @RequestBody UpdateCompletedTaskRequest request,
                                                 Authentication authentication) {
        try {
            User admin = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));
            request.setCompletedTaskId(taskId);
            CompletedTask updated = completedTaskService.updateCompletedTask(request, admin);
            return ResponseEntity.ok(Map.of("message", "Задача успешно обновлена!", "completedTask", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────
    // ИСТОРИЯ ИЗМЕНЕНИЙ
    // ─────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<List<ChangeHistory>> getAllHistory() {
        return ResponseEntity.ok(completedTaskService.getAllHistory());
    }

    @GetMapping("/history/{completedTaskId}")
    public ResponseEntity<List<ChangeHistory>> getTaskHistory(@PathVariable Long completedTaskId) {
        return ResponseEntity.ok(completedTaskService.getTaskHistory(completedTaskId));
    }

    // ─────────────────────────────────────────────────────
    // ПОЛЬЗОВАТЕЛИ
    // ─────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email уже существует!"));
            }
            if (request.getRegion() == null || request.getRegion().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Регион обязателен!"));
            }

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setUsername(request.getUsername());
            user.setRegion(request.getRegion());
            user.setDeleted(false);

            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new RuntimeException("Роль не найдена!"));
            user.setRoles(new HashSet<>(Set.of(role)));

            userRepository.save(user);
            webSocketService.notifyUserCreated();
            return ResponseEntity.ok(Map.of("message", "Пользователь успешно создан!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Transactional
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, Authentication authentication) {
        try {
            User currentUser = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Текущий пользователь не найден!"));
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Нельзя удалить самого себя!"));
            }

            // Удаляем все связанные данные через нативные запросы
            // 1. История изменений где user_id = userId
            userRepository.deleteChangeHistoryByUserId(userId);

            // 2. История изменений где completed_task принадлежит этому пользователю
            userRepository.deleteChangeHistoryByCompletedTaskUserId(userId);

            // 3. Выполненные задачи пользователя
            userRepository.deleteCompletedTasksByUserId(userId);

            // 4. Планы пользователя
            userRepository.deletePlansByUserId(userId);

            // 5. Роли пользователя
            userRepository.deleteUserRoles(userId);

            // 6. Сам пользователь
            userRepository.hardDeleteById(userId);

            webSocketService.notifyUserDeleted();
            return ResponseEntity.ok(Map.of("message", "Пользователь и все его данные удалены!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ─────────────────────────────────────────────────────
    // EXCEL ЭКСПОРТ
    // ─────────────────────────────────────────────────────

    @GetMapping("/export/plans")
    public ResponseEntity<byte[]> exportPlans() throws IOException {
        byte[] excel = excelExportService.exportPlans(planService.getAllPlans());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plans-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/export/completed-tasks")
    public ResponseEntity<byte[]> exportCompletedTasks() throws IOException {
        byte[] excel = excelExportService.exportCompletedTasks(
                completedTaskService.getAllCompletedTasks());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=completed-tasks.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                        @RequestBody RegisterRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

            user.setUsername(request.getUsername());
            user.setRegion(request.getRegion());

            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            if (request.getRole() != null && !request.getRole().isBlank()) {
                Role role = roleRepository.findByName(request.getRole())
                        .orElseThrow(() -> new RuntimeException("Роль не найдена!"));
                user.setRoles(new HashSet<>(Set.of(role)));
            }

            userRepository.save(user);
            webSocketService.notifyUserCreated();
            return ResponseEntity.ok(Map.of("message", "Пользователь успешно обновлён!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}