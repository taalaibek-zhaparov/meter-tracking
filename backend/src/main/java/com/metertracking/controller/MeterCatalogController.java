package com.metertracking.controller;

import com.metertracking.dto.*;
import com.metertracking.service.MeterCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер справочника счётчиков.
 *
 * <p>Эндпоинты:
 * <ul>
 *   <li>{@code POST /api/admin/catalog/import} — импорт справочника из 1С (только ADMIN)</li>
 *   <li>{@code GET  /api/admin/catalog/models} — весь справочник (только ADMIN)</li>
 *   <li>{@code GET  /api/admin/catalog/models/search?q=} — поиск по наименованию</li>
 *   <li>{@code GET  /api/admin/catalog/models/{code}} — модель по коду 1С</li>
 *   <li>{@code POST /api/admin/catalog/devices/{id}/link} — привязать счётчик к модели</li>
 *   <li>{@code GET  /api/user/catalog/models/search?q=} — поиск для монтажника</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MeterCatalogController {

    private final MeterCatalogService catalogService;

    // ─────────────────────────────────────────────────────────────────
    //  ADMIN: Импорт из 1С и управление справочником
    // ─────────────────────────────────────────────────────────────────

    /**
     * Импорт справочника из 1С.
     * Пример тела запроса — см. /resources/example_import.json
     */
    @PostMapping("/api/admin/catalog/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeterImportResponse> importFromOnec(
            @Valid @RequestBody MeterImportRequest request) {
        log.info("[API] POST /api/admin/catalog/import — {} записей", request.items().size());
        MeterImportResponse response = catalogService.importFromOnec(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить весь справочник моделей (для таблицы в админке).
     */
    @GetMapping("/api/admin/catalog/models")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MeterModelDTO>> getAllModels() {
        return ResponseEntity.ok(catalogService.getAllModels());
    }

    /**
     * Поиск модели по наименованию (для автодополнения).
     */
    @GetMapping("/api/admin/catalog/models/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MeterModelDTO>> searchModels(@RequestParam String q) {
        return ResponseEntity.ok(catalogService.searchModels(q));
    }

    /**
     * Получить модель по коду 1С.
     */
    @GetMapping("/api/admin/catalog/models/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getModelByCode(@PathVariable String code) {
        return catalogService.getModelByCode(code)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Привязать счётчик к модели из справочника при регистрации заводского номера.
     * <p>
     * Тело запроса:
     * <pre>{@code
     * {
     *   "modelCode": "МЕР-201-001",
     *   "specId": 3          // необязательно — ID варианта исполнения
     * }
     * }</pre>
     * После вызова в MeterDevice автоматически заполняются:
     * meterType, phases, amperage, znch — из выбранного spec справочника.
     */
    @PostMapping("/api/admin/catalog/devices/{deviceId}/link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> linkDeviceToModel(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Object> body) {
        try {
            String modelCode = (String) body.get("modelCode");
            Long   specId    = body.get("specId") != null
                    ? Long.valueOf(body.get("specId").toString()) : null;

            MeterDeviceDTO result = catalogService.linkDeviceToModel(deviceId, modelCode, specId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  USER: Поиск в справочнике при регистрации
    // ─────────────────────────────────────────────────────────────────

    /**
     * Поиск модели по наименованию — для монтажника при вводе заводского номера.
     * Монтажник вводит тип счётчика → получает список подходящих моделей со spec-ами.
     */
    @GetMapping("/api/user/catalog/models/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<MeterModelDTO>> searchModelsForUser(@RequestParam String q) {
        return ResponseEntity.ok(catalogService.searchModels(q));
    }
}
