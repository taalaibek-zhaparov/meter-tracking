package com.metertracking.service;

import com.metertracking.dto.ReportDTO.*;
import com.metertracking.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        log.info("[REPORT] Формирование дашборда");

        // ── KPI ──────────────────────────────────────────────────────
        long totalPlans = safe(reportRepository.totalPlans());
        long completed  = safe(reportRepository.completedPlans());
        long pending    = totalPlans - completed;
        long masters    = safe(reportRepository.activeMasters());
        long regions    = safe(reportRepository.totalRegions());
        long today      = safe(reportRepository.completedToday());
        long week       = safe(reportRepository.completedThisWeek());
        long month      = safe(reportRepository.completedThisMonth());
        double rate     = totalPlans > 0 ? Math.round(completed * 1000.0 / totalPlans) / 10.0 : 0;
        double avg      = masters    > 0 ? Math.round(completed * 10.0   / masters)    / 10.0 : 0;

        KpiBlock kpi = KpiBlock.builder()
                .totalPlans(totalPlans).completedPlans(completed).pendingPlans(pending)
                .completionRate(rate).totalMasters(masters).totalRegions(regions)
                .avgPerMaster(avg)
                .completedToday(today).completedThisWeek(week).completedThisMonth(month)
                .build();

        // ── РЭС ──────────────────────────────────────────────────────
        List<RegionRow> byRegion = new ArrayList<>();
        try {
            byRegion = reportRepository.regionStats().stream().map(r -> {
                String region = str(r[0]);
                long tot  = num(r[1]), comp = num(r[2]), pend = num(r[3]);
                double pct = tot > 0 ? Math.round(comp * 1000.0 / tot) / 10.0 : 0;
                String status = pct >= 80 ? "GREEN" : pct >= 50 ? "YELLOW" : "RED";
                return RegionRow.builder()
                        .region(region).totalPlans(tot).completed(comp)
                        .pending(pend).rate(pct).status(status).build();
            }).collect(Collectors.toList());
        } catch (Exception e) { log.warn("[REPORT] regionStats error: {}", e.getMessage()); }

        // ── МАСТЕРА ──────────────────────────────────────────────────
        List<MasterRow> byMaster = new ArrayList<>();
        try {
            byMaster = reportRepository.masterStats().stream().map(r -> {
                String name = str(r[0]), reg = str(r[1]);
                long tot = num(r[2]), comp = num(r[3]), pend = num(r[4]);
                String last = r[5] != null ? r[5].toString() : "—";
                double pct = tot > 0 ? Math.round(comp * 1000.0 / tot) / 10.0 : 0;
                return MasterRow.builder()
                        .masterName(name).region(reg).total(tot).completed(comp)
                        .pending(pend).rate(pct).lastActivity(last).build();
            }).collect(Collectors.toList());
        } catch (Exception e) { log.warn("[REPORT] masterStats error: {}", e.getMessage()); }

        // ── ТИПЫ СЧЁТЧИКОВ ───────────────────────────────────────────
        List<MeterTypeRow> byMeterType = new ArrayList<>();
        try {
            List<Object[]> rawTypes = reportRepository.meterTypeStats();
            long totalInst = rawTypes.stream().mapToLong(r -> num(r[1])).sum();
            byMeterType = rawTypes.stream().map(r -> {
                long cnt = num(r[1]);
                double pct = totalInst > 0 ? Math.round(cnt * 1000.0 / totalInst) / 10.0 : 0;
                return MeterTypeRow.builder().meterType(str(r[0])).count(cnt).percent(pct).build();
            }).collect(Collectors.toList());
        } catch (Exception e) { log.warn("[REPORT] meterTypeStats error: {}", e.getMessage()); }

        // ── ДИНАМИКА ─────────────────────────────────────────────────
        List<DayPoint> daily = new ArrayList<>();
        try {
            daily = reportRepository.dailyDynamics().stream()
                    .map(r -> DayPoint.builder().date(str(r[0])).count(num(r[1])).build())
                    .collect(Collectors.toList());
        } catch (Exception e) { log.warn("[REPORT] dailyDynamics error: {}", e.getMessage()); }

        // ── ТОП / АНТИТОП ────────────────────────────────────────────
        TopBlock top = buildTop(byRegion, byMaster);

        // ── ПРОБЛЕМЫ ─────────────────────────────────────────────────
        List<String> problems = detectProblems(byRegion, byMaster, kpi);

        return DashboardResponse.builder()
                .kpi(kpi).byRegion(byRegion).byMaster(byMaster)
                .byMeterType(byMeterType).dailyDynamics(daily)
                .problems(problems).top(top).build();
    }

    private TopBlock buildTop(List<RegionRow> regions, List<MasterRow> masters) {
        List<RegionRow> rWithPlans = regions.stream().filter(r -> r.getTotalPlans() > 0).collect(Collectors.toList());
        List<MasterRow> mWithTasks = masters.stream().filter(m -> m.getTotal()      > 0).collect(Collectors.toList());

        return TopBlock.builder()
                .topRegion(rWithPlans.stream().max(Comparator.comparingDouble(RegionRow::getRate)).orElse(null))
                .bottomRegion(rWithPlans.stream().min(Comparator.comparingDouble(RegionRow::getRate)).orElse(null))
                .topMaster(mWithTasks.stream().max(Comparator.comparingLong(MasterRow::getCompleted)).orElse(null))
                .bottomMaster(mWithTasks.stream()
                        .filter(m -> m.getCompleted() == 0 && m.getPending() > 0)
                        .max(Comparator.comparingLong(MasterRow::getPending)).orElse(null))
                .build();
    }

    private List<String> detectProblems(List<RegionRow> regions, List<MasterRow> masters, KpiBlock kpi) {
        List<String> problems = new ArrayList<>();

        regions.stream().filter(r -> r.getRate() < 30 && r.getTotalPlans() > 0)
                .forEach(r -> problems.add("🔴 РЭС \"" + r.getRegion() + "\": выполнено " + r.getRate() + "% (" + r.getCompleted() + "/" + r.getTotalPlans() + ")"));

        regions.stream().filter(r -> r.getCompleted() == 0 && r.getTotalPlans() > 5)
                .forEach(r -> problems.add("⛔ РЭС \"" + r.getRegion() + "\": нет выполненных из " + r.getTotalPlans()));

        masters.stream().filter(m -> m.getCompleted() == 0 && m.getTotal() > 0)
                .forEach(m -> problems.add("👤 Мастер \"" + m.getMasterName() + "\" (" + m.getRegion() + "): " + m.getTotal() + " задач, не выполнено ни одной"));

        if (kpi.getCompletionRate() < 50)
            problems.add("📊 Общий % выполнения ниже нормы: " + kpi.getCompletionRate() + "%");

        if (problems.isEmpty())
            problems.add("✅ Критических проблем не обнаружено");

        return problems;
    }

    private long safe(Long v) { return v != null ? v : 0L; }
    private long num(Object o) { return o != null ? ((Number) o).longValue() : 0L; }
    private String str(Object o) { return o != null ? o.toString() : "—"; }
}
