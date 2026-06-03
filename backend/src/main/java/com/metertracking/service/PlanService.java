package com.metertracking.service;

import com.metertracking.dto.OneCMeterReadingDTO;
import com.metertracking.dto.PlanRequest;
import com.metertracking.entity.Plan;
import com.metertracking.entity.User;
import com.metertracking.repository.PlanRepository;
import com.metertracking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис управления планами замены счётчиков.
 *
 * ✅ ИЗМЕНЕНО: OneCComService → OneCHttpService (HTTP Web-публикация 1С)
 *
 * Поток создания плана:
 * 1. Находим пользователя по userId
 * 2. Если введён licevoy — запрашиваем данные из 1С через HTTP GET
 * 3. Если 1С вернул данные — используем их (tp, tip, counterNo, pkz, datepkz, fio, adres)
 * 4. Если 1С недоступен — используем данные из запроса (ручной ввод)
 * 5. Сохраняем план и уведомляем через WebSocket
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final OneCHttpService oneCHttpService; // ✅ HTTP вместо COM

    /**
     * Создаёт новый план замены счётчика.
     *
     * Автозаполнение из 1С:
     * Если licevoy заполнен — делает GET запрос к 1С HTTP публикации.
     * Данные из 1С имеют приоритет над данными из формы.
     * Новые поля fio и adres берутся только из 1С.
     */
    @Transactional
    public Plan createPlan(PlanRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        Plan plan = new Plan();
        plan.setUser(user);

        if (request.getLicevoy() != null && !request.getLicevoy().isBlank()) {
            Optional<OneCMeterReadingDTO> onecData = oneCHttpService.getLastMeterInfo(request.getLicevoy());

            if (onecData.isPresent()) {
                OneCMeterReadingDTO d = onecData.get();
                // Данные из 1С имеют приоритет, fallback на данные из запроса
                plan.setRegion(d.getRegion() != null ? d.getRegion() : request.getRegion());
                plan.setTp(d.getTp() != null ? d.getTp() : request.getTp());
                plan.setLicevoy(d.getAccount() != null ? d.getAccount() : request.getLicevoy());
                plan.setTip(d.getMeterType() != null ? d.getMeterType() : request.getTip());
                plan.setNomerSchetchika(d.getMeterNumber() != null ? d.getMeterNumber() : request.getNomerSchetchika());
                plan.setPokazaniya(d.getReading() != null ? d.getReading() : request.getPokazaniya());
                plan.setData(d.getDate() != null ? d.getDate() : request.getData());
                // ✅ НОВЫЕ ПОЛЯ из 1С HTTP
                plan.setFio(d.getFio());
                plan.setAdres(d.getAdres());
                plan.setDocumentType(request.getDocumentType());
            } else {
                setFromRequest(plan, request);
            }
        } else {
            setFromRequest(plan, request);
        }

        plan.setCompleted(false);
        Plan saved = planRepository.save(plan);
        webSocketService.notifyPlanCreated();
        return saved;
    }

    private void setFromRequest(Plan plan, PlanRequest request) {
        plan.setRegion(request.getRegion());
        plan.setTp(request.getTp());
        plan.setLicevoy(request.getLicevoy());
        plan.setTip(request.getTip());
        plan.setNomerSchetchika(request.getNomerSchetchika());
        plan.setPokazaniya(request.getPokazaniya());
        plan.setData(request.getData());
        plan.setFio(request.getFio());
        plan.setAdres(request.getAdres());
        plan.setDocumentType(request.getDocumentType());
    }

    /** Обновляет существующий план. Только для администратора. */
    @Transactional
    public Plan updatePlan(Long planId, PlanRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("План не найден!"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден!"));

        plan.setUser(user);
        plan.setRegion(request.getRegion());
        plan.setTp(request.getTp());
        plan.setLicevoy(request.getLicevoy());
        plan.setTip(request.getTip());
        plan.setNomerSchetchika(request.getNomerSchetchika());
        plan.setPokazaniya(request.getPokazaniya());
        plan.setData(request.getData());
        plan.setFio(request.getFio());
        plan.setAdres(request.getAdres());

        Plan updated = planRepository.save(plan);
        webSocketService.notifyPlanUpdated();
        return updated;
    }

    /**
     * Удаляет план. Выполненные планы удалять нельзя.
     */
    @Transactional
    public void deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("План не найден!"));
        if (plan.getCompleted()) {
            throw new RuntimeException("Нельзя удалить выполненный план!");
        }
        planRepository.delete(plan);
        webSocketService.notifyPlanDeleted();
    }


    /** Все планы конкретного РЭС — для AdminRes. */
    @Transactional(readOnly = true)
    public List<Plan> getPlansByRegion(String region) {
        return planRepository.findAllByRegion(region);
    }

    /** Получить план по ID. */
    @Transactional(readOnly = true)
    public Plan getPlanById(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("План не найден: " + planId));
    }

    /** Все планы для администратора. */
    @Transactional(readOnly = true)
    public List<Plan> getAllPlans() {
        return planRepository.findAllWithRelations();
    }

    /** Активные задачи пользователя (completed = false). */
    @Transactional(readOnly = true)
    public List<Plan> getIncompletePlansByUser(User user) {
        return planRepository.findByUserAndCompletedFalse(user);
    }

    /** Все планы пользователя включая выполненные. */
    @Transactional(readOnly = true)
    public List<Plan> getPlansByUser(User user) {
        return planRepository.findByUser(user);
    }
}