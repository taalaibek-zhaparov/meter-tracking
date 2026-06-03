package com.metertracking.service;

import com.metertracking.dto.CompletedTaskRequest;
import com.metertracking.dto.UpdateCompletedTaskRequest;
import com.metertracking.entity.ChangeHistory;
import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.Plan;
import com.metertracking.entity.User;
import com.metertracking.repository.ChangeHistoryRepository;
import com.metertracking.repository.CompletedTaskRepository;
import com.metertracking.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.metertracking.repository.MeterDeviceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompletedTaskService {

    private final CompletedTaskRepository completedTaskRepository;
    private final PlanRepository planRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final WebSocketService webSocketService;
    private final OneCHttpService oneCHttpService;
    private final MeterDeviceRepository meterDeviceRepository;

    /**
     * Выполняет задачу пользователем.
     *
     * ✅ ИЗМЕНЕНО: fio и adres берутся из плана (куда они попали из 1С при создании),
     * либо из запроса если в плане их нет.
     */
    @Transactional
    public CompletedTask completeTask(CompletedTaskRequest request, User user) {
        Plan plan = planRepository.findByIdForUpdate(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("План не найден!"));

        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Вы не можете выполнить эту задачу!");
        }
        if (plan.getCompleted()) {
            throw new RuntimeException("Задача уже выполнена!");
        }

        validateTaskData(request);

        CompletedTask completedTask = new CompletedTask();
        completedTask.setPlan(plan);
        completedTask.setUser(user);
        completedTask.setRegion(request.getRegion());
        completedTask.setTp(request.getTp());
        completedTask.setLicevoy(request.getLicevoy());
        completedTask.setTip(request.getTip());
        completedTask.setNomerSchetchika(request.getNomerSchetchika());
        completedTask.setPokazaniya(request.getPokazaniya());
        completedTask.setData(request.getData());
        completedTask.setNomerPlomby(request.getNomerPlomby());
        completedTask.setNomerSimKarty(request.getNomerSimKarty());
        completedTask.setNomerIccid(request.getNomerIccid());
        completedTask.setOldNomerSchetchika(request.getOldNomerSchetchika());
        completedTask.setOldPokazaniya(request.getOldPokazaniya());
        completedTask.setOldMeterType(plan.getTip());
        completedTask.setNewPokazaniya(request.getNewPokazaniya() != null ? request.getNewPokazaniya() : 0.0);
        completedTask.setPlombaGos(request.getPlombaGos());
        completedTask.setNaKryshke(request.getNaKryshke());
        completedTask.setNaYashike(request.getNaYashike());
        completedTask.setPhases(request.getNewPhases());
        completedTask.setAmperage(request.getNewAmperage());
        completedTask.setZnch(request.getZnch());
        completedTask.setSignatureAbonent(request.getSignatureAbonent());
        completedTask.setSignatureMaster(request.getSignatureMaster());

        // Полный формат тока из справочника: "5(80)А" — для корректной отправки в 1С
        completedTask.setAmpSpec(request.getAmpSpec());
        // Числовой код модели в 1С — берём из справочника по номеру нового счётчика
        int resolvedMeterCode = 0;
        if (request.getMeterCode() != null && request.getMeterCode() > 0) {
            resolvedMeterCode = request.getMeterCode();
        } else if (request.getNomerSchetchika() != null) {
            // Автоматически ищем код в базе счётчиков
            meterDeviceRepository.findByMeterNumber(request.getNomerSchetchika())
                    .ifPresent(device -> {
                        if (device.getMeterModel() != null && device.getMeterModel().getCode() != null) {
                            try {
                                completedTask.setMeterCode(Integer.parseInt(device.getMeterModel().getCode()));
                                log.info("MeterCode для {} = {}", request.getNomerSchetchika(), device.getMeterModel().getCode());
                            } catch (NumberFormatException e) {
                                log.warn("Код модели не число: {}", device.getMeterModel().getCode());
                            }
                        }
                    });
        }
        if (completedTask.getMeterCode() == null || completedTask.getMeterCode() == 0) {
            completedTask.setMeterCode(resolvedMeterCode);
        }
        // Долг абонента из 1С (Summa)
        completedTask.setSumma(request.getSumma() != null ? request.getSumma() : 0.0);
        // Kwt рассчитывается автоматически в OneCHttpService

        // fio и adres: берём из плана (там данные из 1С), fallback на request
        completedTask.setFio(plan.getFio() != null ? plan.getFio() : request.getFio());
        completedTask.setAdres(plan.getAdres() != null ? plan.getAdres() : request.getAdres());
        completedTask.setDocumentType(
                plan.getDocumentType() != null ? plan.getDocumentType() : request.getDocumentType()
        );

        completedTask.setCreatedAt(LocalDateTime.now());
        completedTask.setUpdatedAt(LocalDateTime.now());
        completedTask.setUpdatedBy(user.getId());


        plan.setCompleted(true);
        planRepository.save(plan);

        Long count = completedTaskRepository.countAll();
        String docNumber = "204" + String.format("%06d", count + 1);
        completedTask.setDocumentNumber(docNumber);

        CompletedTask saved = completedTaskRepository.save(completedTask);
        recordHistory(saved, user, "CREATE", "Задача создана", null, "Создана");
        // ✅ Отправляем в 1С
        try {
            oneCHttpService.sendCompletedTask(saved);
        } catch (Exception e) {
            log.warn("Не удалось отправить в 1С: {}", e.getMessage());
        }
        webSocketService.notifyTaskCompleted();
        return saved;
    }

    /**
     * Обновляет выполненную задачу.
     */
    @Transactional
    public CompletedTask updateCompletedTask(UpdateCompletedTaskRequest request, User user) {
        CompletedTask task = completedTaskRepository.findByIdWithRelations(request.getCompletedTaskId())
                .orElseThrow(() -> new RuntimeException("Выполненная задача не найдена!"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        if (!isAdmin && !task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Вы не можете редактировать эту задачу!");
        }

        validateUpdateData(request);
        trackChanges(task, request, user);

        task.setRegion(request.getRegion());
        task.setTp(request.getTp());
        task.setLicevoy(request.getLicevoy());
        task.setTip(request.getTip());
        task.setNomerSchetchika(request.getNomerSchetchika());
        task.setPokazaniya(request.getPokazaniya());
        task.setData(request.getData());
        task.setNomerPlomby(request.getNomerPlomby());
        task.setNomerSimKarty(request.getNomerSimKarty());
        task.setNomerIccid(request.getNomerIccid());
        if (request.getNaKryshke() != null) task.setNaKryshke(request.getNaKryshke());
        if (request.getNaYashike() != null) task.setNaYashike(request.getNaYashike());
        if (request.getPlombaGos() != null) task.setPlombaGos(request.getPlombaGos());
        task.setUpdatedAt(LocalDateTime.now());
        task.setUpdatedBy(user.getId());

        CompletedTask saved = completedTaskRepository.save(task);
        webSocketService.notifyTaskUpdated();
        return saved;
    }

    // ── ВАЛИДАЦИЯ ──────────────────────────────────────────

    private void validateTaskData(CompletedTaskRequest request) {
        if (request.getPokazaniya() != null && request.getPokazaniya() < 0)
            throw new RuntimeException("Показания не могут быть отрицательными!");
        if (request.getData() != null && request.getData().isAfter(LocalDate.now()))
            throw new RuntimeException("Дата не может быть в будущем!");
        if (request.getRegion() == null || request.getRegion().isBlank())
            throw new RuntimeException("Регион обязателен!");
        if (request.getTp() == null || request.getTp().isBlank())
            throw new RuntimeException("ТП обязателен!");
        if (request.getNomerPlomby() == null || request.getNomerPlomby().isBlank())
            throw new RuntimeException("Одноразовая пломба обязательна!");
        if (request.getNomerSimKarty() == null || request.getNomerSimKarty().isBlank())
            throw new RuntimeException("Номер SIM-карты обязателен!");
        if (request.getNomerIccid() == null || request.getNomerIccid().isBlank())
            throw new RuntimeException("Номер ICCID обязателен!");
    }

    private void validateUpdateData(UpdateCompletedTaskRequest request) {
        if (request.getPokazaniya() != null && request.getPokazaniya() < 0)
            throw new RuntimeException("Показания не могут быть отрицательными!");
        if (request.getData() != null && request.getData().isAfter(LocalDate.now()))
            throw new RuntimeException("Дата не может быть в будущем!");
    }

    // ── ИСТОРИЯ ИЗМЕНЕНИЙ ──────────────────────────────────

    private void trackChanges(CompletedTask old, UpdateCompletedTaskRequest n, User user) {
        if (!Objects.equals(old.getRegion(), n.getRegion()))
            recordHistory(old, user, "UPDATE", "region", old.getRegion(), n.getRegion());
        if (!Objects.equals(old.getTp(), n.getTp()))
            recordHistory(old, user, "UPDATE", "tp", old.getTp(), n.getTp());
        if (!Objects.equals(old.getLicevoy(), n.getLicevoy()))
            recordHistory(old, user, "UPDATE", "licevoy", old.getLicevoy(), n.getLicevoy());
        if (!Objects.equals(old.getTip(), n.getTip()))
            recordHistory(old, user, "UPDATE", "tip", old.getTip(), n.getTip());
        if (!Objects.equals(old.getNomerSchetchika(), n.getNomerSchetchika()))
            recordHistory(old, user, "UPDATE", "nomerSchetchika", old.getNomerSchetchika(), n.getNomerSchetchika());
        if (!Objects.equals(old.getPokazaniya(), n.getPokazaniya()))
            recordHistory(old, user, "UPDATE", "pokazaniya",
                    old.getPokazaniya() != null ? old.getPokazaniya().toString() : null,
                    n.getPokazaniya() != null ? n.getPokazaniya().toString() : null);
        if (!Objects.equals(old.getData(), n.getData()))
            recordHistory(old, user, "UPDATE", "data",
                    old.getData() != null ? old.getData().toString() : null,
                    n.getData() != null ? n.getData().toString() : null);
        if (!Objects.equals(old.getNomerPlomby(), n.getNomerPlomby()))
            recordHistory(old, user, "UPDATE", "nomerPlomby", old.getNomerPlomby(), n.getNomerPlomby());
        if (!Objects.equals(old.getNomerSimKarty(), n.getNomerSimKarty()))
            recordHistory(old, user, "UPDATE", "nomerSimKarty", old.getNomerSimKarty(), n.getNomerSimKarty());
        if (!Objects.equals(old.getNomerIccid(), n.getNomerIccid()))
            recordHistory(old, user, "UPDATE", "nomerIccid", old.getNomerIccid(), n.getNomerIccid());
        if (!Objects.equals(old.getNaKryshke(), n.getNaKryshke()))
            recordHistory(old, user, "UPDATE", "naKryshke", old.getNaKryshke(), n.getNaKryshke());
        if (!Objects.equals(old.getNaYashike(), n.getNaYashike()))
            recordHistory(old, user, "UPDATE", "naYashike", old.getNaYashike(), n.getNaYashike());
        if (!Objects.equals(old.getPlombaGos(), n.getPlombaGos()))
            recordHistory(old, user, "UPDATE", "plombaGos", old.getPlombaGos(), n.getPlombaGos());
    }

    private void recordHistory(CompletedTask task, User user, String actionType,
                               String fieldName, String oldValue, String newValue) {
        ChangeHistory history = new ChangeHistory();
        history.setCompletedTask(task);
        history.setUser(user);
        history.setUserName(user.getUsername());
        history.setChangeTime(LocalDateTime.now());
        history.setActionType(actionType);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        changeHistoryRepository.save(history);
    }

    // ── ЧТЕНИЕ ────────────────────────────────────────────


    /** Выполненные задачи конкретного РЭС — для AdminRes. */
    @Transactional(readOnly = true)
    public List<CompletedTask> getCompletedTasksByRegion(String region) {
        return completedTaskRepository.findAllByRegion(region);
    }

    @Transactional(readOnly = true)
    public List<CompletedTask> getAllCompletedTasks() {
        return completedTaskRepository.findAllWithRelations();
    }

    @Transactional(readOnly = true)
    public List<CompletedTask> getCompletedTasksByUser(User user) {
        return completedTaskRepository.findByUserWithRelations(user);
    }

    @Transactional(readOnly = true)
    public List<ChangeHistory> getTaskHistory(Long completedTaskId) {
        CompletedTask task = completedTaskRepository.findById(completedTaskId)
                .orElseThrow(() -> new RuntimeException("Задача не найдена!"));
        return changeHistoryRepository.findByCompletedTaskOrderByChangeTimeDesc(task);
    }

    @Transactional(readOnly = true)
    public List<ChangeHistory> getAllHistory() {
        return changeHistoryRepository.findAllByOrderByChangeTimeDesc();
    }
}