package com.metertracking.service;

import com.metertracking.dto.*;
import com.metertracking.entity.MeterDevice;
import com.metertracking.entity.MeterModel;
import com.metertracking.entity.MeterSpec;
import com.metertracking.repository.MeterDeviceRepository;
import com.metertracking.repository.MeterModelRepository;
import com.metertracking.repository.MeterSpecRepository;
import com.metertracking.util.MeterNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис справочника счётчиков.
 *
 * <p>Две задачи:
 * <ol>
 *   <li>Импорт справочника из 1С (batch-операции, append-only).</li>
 *   <li>Поиск модели по коду/наименованию при регистрации заводского номера:
 *       когда монтажник вводит заводской номер счётчика, система находит
 *       подходящую модель из справочника и автоматически заполняет
 *       характеристики (тип, фазность, ампераж, значность).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeterCatalogService {

    private final MeterModelRepository    modelRepo;
    private final MeterSpecRepository     specRepo;
    private final MeterDeviceRepository   deviceRepo;

    // ─────────────────────────────────────────────────────────────────
    //  1. Импорт из 1С
    // ─────────────────────────────────────────────────────────────────

    /**
     * Импортирует список счётчиков из 1С (append-only).
     * Существующие записи не изменяются — только добавляются новые.
     */
    @Transactional
    public MeterImportResponse importFromOnec(MeterImportRequest request) {

        List<MeterImportItem> items = request.items();
        log.info("[CATALOG] Импорт из 1С: {} записей", items.size());

        // ── Шаг 1: собрать все коды из входных данных ─────────────────
        Set<String> incomingCodes = new HashSet<>();
        for (MeterImportItem item : items) {
            if (StringUtils.hasText(item.code())) {
                incomingCodes.add(item.code().trim());
            }
        }

        // ── Шаг 2: batch-загрузить существующие модели ─────────────────
        Map<String, MeterModel> existingByCode = new HashMap<>();
        for (MeterModel m : modelRepo.findAllByCodes(incomingCodes)) {
            existingByCode.put(m.getCode(), m);
        }

        int modelsCreated = 0, modelsSkipped = 0;
        int specsCreated  = 0, specsSkipped  = 0, specsInvalid = 0;

        List<MeterModel> newModels  = new ArrayList<>();
        Map<String, MeterModel> modelByCode = new HashMap<>(existingByCode);

        // ── Шаг 3: обработать модели ───────────────────────────────────
        for (MeterImportItem item : items) {
            if (!StringUtils.hasText(item.code())) {
                log.warn("[CATALOG] Пропущена запись без кода: name='{}'", item.name());
                specsInvalid++;
                continue;
            }
            String code = item.code().trim();
            MeterModel model = modelByCode.get(code);
            if (model == null) {
                model = MeterModel.builder()
                        .code(code)
                        .name(MeterNormalizer.normalizeName(item.name()))
                        .build();
                newModels.add(model);
                modelByCode.put(code, model);
                modelsCreated++;
            } else {
                modelsSkipped++;
                String normalized = MeterNormalizer.normalizeName(item.name());
                if (!Objects.equals(model.getName(), normalized)) {
                    log.info("[CATALOG] Модель code='{}' существует. name='{}' → '{}' (игнорируется)",
                            code, model.getName(), normalized);
                }
            }
        }

        // ── Шаг 4: batch-сохранить новые модели ───────────────────────
        if (!newModels.isEmpty()) {
            modelRepo.saveAll(newModels);
            log.info("[CATALOG] Сохранено {} новых моделей", newModels.size());
        }

        // ── Шаг 5: загрузить существующие specs ────────────────────────
        Set<Long> affectedModelIds = new HashSet<>();
        for (MeterImportItem item : items) {
            if (StringUtils.hasText(item.code())) {
                MeterModel m = modelByCode.get(item.code().trim());
                if (m != null && m.getId() != null) {
                    affectedModelIds.add(m.getId());
                }
            }
        }

        Set<String> existingSpecKeys = new HashSet<>();
        if (!affectedModelIds.isEmpty()) {
            for (MeterSpec s : specRepo.findAllByModelIds(affectedModelIds)) {
                existingSpecKeys.add(specKey(s.getMeterModel().getId(),
                        s.getAmp(), s.getDigits(), s.getPhase(), s.getVoltage()));
            }
        }

        // ── Шаг 6: обработать specs ────────────────────────────────────
        List<MeterSpec> newSpecs = new ArrayList<>();

        for (MeterImportItem item : items) {
            if (!StringUtils.hasText(item.code())) continue;
            MeterModel model = modelByCode.get(item.code().trim());
            if (model == null) continue;

            String amp     = MeterNormalizer.normalizeAmp(item.amp());
            String voltage = MeterNormalizer.normalizeVoltage(item.voltage());
            Short  digits  = item.digits();
            Short  phase   = item.phase();

            if (!StringUtils.hasText(amp) || !StringUtils.hasText(voltage)
                    || digits == null || phase == null) {
                log.warn("[CATALOG] Неполные данные spec code='{}': amp={}, digits={}, phase={}, voltage={}",
                        item.code(), amp, digits, phase, voltage);
                specsInvalid++;
                continue;
            }

            Long   modelId = model.getId();
            String key = (modelId != null)
                    ? specKey(modelId, amp, digits, phase, voltage)
                    : specKey(Objects.hash(model.getCode()), amp, digits, phase, voltage);

            if (existingSpecKeys.contains(key)) {
                specsSkipped++;
                continue;
            }
            existingSpecKeys.add(key);

            newSpecs.add(MeterSpec.builder()
                    .meterModel(model)
                    .amp(amp)
                    .digits(digits)
                    .phase(phase)
                    .voltage(voltage)
                    .build());
            specsCreated++;
        }

        // ── Шаг 7: batch-сохранить specs ──────────────────────────────
        if (!newSpecs.isEmpty()) {
            specRepo.saveAll(newSpecs);
            log.info("[CATALOG] Сохранено {} новых вариантов исполнения", newSpecs.size());
        }

        MeterImportResponse response = new MeterImportResponse(
                items.size(), modelsCreated, modelsSkipped,
                specsCreated, specsSkipped, specsInvalid);
        log.info("[CATALOG] Импорт завершён: {}", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. Поиск в справочнике при регистрации заводского номера
    // ─────────────────────────────────────────────────────────────────

    /**
     * Поиск моделей по наименованию (подсказка при вводе типа счётчика).
     * Возвращает до 10 результатов.
     */
    @Transactional(readOnly = true)
    public List<MeterModelDTO> searchModels(String query) {
        return modelRepo.searchByName(query, PageRequest.of(0, 10))
                .stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить модель по коду из 1С.
     */
    @Transactional(readOnly = true)
    public Optional<MeterModelDTO> getModelByCode(String code) {
        return modelRepo.findByCode(code).map(this::toModelDTO);
    }

    /**
     * Получить все модели справочника (для таблицы в админке).
     */
    @Transactional(readOnly = true)
    public List<MeterModelDTO> getAllModels() {
        return modelRepo.findAll().stream()
                .map(this::toModelDTO)
                .collect(Collectors.toList());
    }

    /**
     * Привязать счётчик (MeterDevice) к модели из справочника.
     * Вызывается при регистрации заводского номера:
     * после ввода номера система ищет подходящую модель
     * и заполняет характеристики из первого подходящего spec.
     *
     * @param deviceId    ID записи в meter_devices
     * @param modelCode   Код модели из справочника 1С
     * @param specId      ID конкретного варианта исполнения (опционально)
     * @return обновлённый MeterDeviceDTO
     */
    @Transactional
    public MeterDeviceDTO linkDeviceToModel(Long deviceId, String modelCode, Long specId) {
        MeterDevice device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Счётчик не найден: " + deviceId));

        MeterModel model = modelRepo.findByCode(modelCode)
                .orElseThrow(() -> new RuntimeException("Модель не найдена в справочнике: " + modelCode));

        device.setMeterModel(model);

        // Если передан specId — заполняем характеристики из spec
        if (specId != null) {
            MeterSpec spec = model.getSpecs().stream()
                    .filter(s -> s.getId().equals(specId))
                    .findFirst()
                    .orElseGet(() -> specRepo.findById(specId).orElse(null));

            if (spec != null) {
                device.setMeterType(model.getName());
                device.setPhases(spec.getPhase().intValue());
                device.setAmperage(parseAmperage(spec.getAmp()));
                device.setZnch(spec.getDigits().intValue());
                device.setVoltage(spec.getVoltage());
                log.info("[CATALOG] Счётчик id={} привязан к модели '{}' spec id={}",
                        deviceId, model.getName(), specId);
            }
        } else {
            // Без specId — заполняем только тип
            device.setMeterType(model.getName());
            log.info("[CATALOG] Счётчик id={} привязан к модели '{}' (без spec)",
                    deviceId, model.getName());
        }

        MeterDevice saved = deviceRepo.save(device);
        return toDeviceDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Хелперы
    // ─────────────────────────────────────────────────────────────────

    private MeterModelDTO toModelDTO(MeterModel m) {
        List<MeterSpecDTO> specs = m.getSpecs().stream()
                .map(s -> new MeterSpecDTO(s.getId(), s.getAmp(),
                        s.getDigits(), s.getPhase(), s.getVoltage()))
                .collect(Collectors.toList());
        return new MeterModelDTO(m.getId(), m.getCode(), m.getName(), specs);
    }

    private MeterDeviceDTO toDeviceDTO(MeterDevice d) {
        MeterDeviceDTO dto = new MeterDeviceDTO();
        dto.setId(d.getId());
        dto.setMeterNumber(d.getMeterNumber());
        dto.setMeterType(d.getMeterType());
        dto.setSimCardNumber(d.getSimCardNumber());
        dto.setIccidNumber(d.getIccidNumber());
        dto.setSealNumber(d.getSealNumber());
        dto.setPhases(d.getPhases());
        dto.setAmperage(d.getAmperage());
        dto.setZnch(d.getZnch());
        dto.setAvailable(d.isAvailable());
        if (d.getMeterModel() != null) {
            dto.setMeterModelCode(d.getMeterModel().getCode());
            dto.setMeterModelName(d.getMeterModel().getName());
        }
        return dto;
    }

    /**
     * Извлекает первое число из строки ампеража.
     * "5(80)A" → 5, "100A" → 100.
     */
    private Integer parseAmperage(String amp) {
        if (!StringUtils.hasText(amp)) return null;
        try {
            String digits = amp.replaceAll("[^0-9].*", ""); // взять всё до первого нецифрового
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String specKey(long modelId, String amp, Short digits,
                                  Short phase, String voltage) {
        return modelId + "|" + amp + "|" + digits + "|" + phase + "|" + voltage;
    }

    private static String specKey(int hashCode, String amp, Short digits,
                                  Short phase, String voltage) {
        return "h" + hashCode + "|" + amp + "|" + digits + "|" + phase + "|" + voltage;
    }
}
