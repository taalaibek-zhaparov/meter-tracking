package com.metertracking.controller;

import com.metertracking.dto.CompletedTaskRequest;
import com.metertracking.dto.MeterDeviceDTO;
import com.metertracking.dto.UpdateCompletedTaskRequest;
import com.metertracking.entity.ChangeHistory;
import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.Plan;
import com.metertracking.entity.User;
import com.metertracking.repository.UserRepository;
import com.metertracking.service.CompletedTaskService;
import com.metertracking.service.ExcelExportService;
import com.metertracking.service.MeterDeviceService;
import com.metertracking.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ✅ ИСПРАВЛЕНО: убран @CrossOrigin(origins = "*") — CORS уже настроен в SecurityConfig
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final PlanService planService;
    private final CompletedTaskService completedTaskService;
    private final UserRepository userRepository;
    private final MeterDeviceService meterDeviceService;
    private final ExcelExportService excelExportService;

    // ─────────────────────────────────────────────────────
    // МОИ ЗАДАЧИ
    // ─────────────────────────────────────────────────────

    @GetMapping("/my-tasks")
    public ResponseEntity<List<Plan>> getMyTasks(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(planService.getIncompletePlansByUser(user));
    }

    @GetMapping("/my-all-plans")
    public ResponseEntity<List<Plan>> getMyAllPlans(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(planService.getPlansByUser(user));
    }

    @PostMapping("/complete-task")
    public ResponseEntity<?> completeTask(@RequestBody CompletedTaskRequest request,
                                          Authentication authentication) {
        try {
            User user = getUser(authentication);
            CompletedTask completedTask = completedTaskService.completeTask(request, user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Данные успешно записались в БД!");
            response.put("completedTask", completedTask);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update-task")
    public ResponseEntity<?> updateCompletedTask(@RequestBody UpdateCompletedTaskRequest request,
                                                 Authentication authentication) {
        try {
            User user = getUser(authentication);
            CompletedTask updatedTask = completedTaskService.updateCompletedTask(request, user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Задача успешно обновлена!");
            response.put("completedTask", updatedTask);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-completed-tasks")
    public ResponseEntity<List<CompletedTask>> getMyCompletedTasks(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(completedTaskService.getCompletedTasksByUser(user));
    }

    @GetMapping("/task-history/{completedTaskId}")
    public ResponseEntity<List<ChangeHistory>> getTaskHistory(@PathVariable Long completedTaskId) {
        return ResponseEntity.ok(completedTaskService.getTaskHistory(completedTaskId));
    }

    // ─────────────────────────────────────────────────────
    // ПОИСК СЧЁТЧИКОВ
    // ─────────────────────────────────────────────────────

    @GetMapping("/meters/search")
    public ResponseEntity<List<MeterDeviceDTO>> searchMeters(@RequestParam String query) {
        return ResponseEntity.ok(meterDeviceService.searchByMeterNumber(query));
    }

    @GetMapping("/meters/{meterNumber}")
    public ResponseEntity<?> getMeterByNumber(@PathVariable String meterNumber) {
        return meterDeviceService.getByMeterNumber(meterNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Отмечает счётчик как "Занят" при выборе мастером.
     * PUT /api/user/meters/{meterNumber}/mark-busy
     */
    @PutMapping("/meters/{meterNumber}/mark-busy")
    public ResponseEntity<?> markMeterBusy(@PathVariable String meterNumber) {
        try {
            meterDeviceService.getByMeterNumber(meterNumber).ifPresent(dto -> {
                var device = meterDeviceService.findEntityByNumber(meterNumber);
                device.ifPresent(d -> {
                    d.setAvailable(false);
                    meterDeviceService.saveEntity(d);
                });
            });
            return ResponseEntity.ok(java.util.Map.of("message", "Статус обновлён"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────
    // EXCEL ЭКСПОРТ
    // ─────────────────────────────────────────────────────

    @GetMapping("/export/my-plans")
    public ResponseEntity<byte[]> exportMyPlans(Authentication authentication) throws IOException {
        User user = getUser(authentication);
        List<Plan> plans = planService.getPlansByUser(user);
        byte[] excel = excelExportService.exportPlans(plans);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-plans.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/export/my-completed-tasks")
    public ResponseEntity<byte[]> exportMyCompletedTasks(Authentication authentication) throws IOException {
        User user = getUser(authentication);
        List<CompletedTask> tasks = completedTaskService.getCompletedTasksByUser(user);
        byte[] excel = excelExportService.exportCompletedTasks(tasks);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-completed-tasks.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // ─────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));
    }
}
