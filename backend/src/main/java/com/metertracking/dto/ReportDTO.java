package com.metertracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Все DTO для раздела "Отчёты".
 * Один файл — все структуры данных дашборда.
 */
public class ReportDTO {

    /** Главный ответ — весь дашборд одним запросом */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private KpiBlock kpi;
        private List<RegionRow> byRegion;
        private List<MasterRow> byMaster;
        private List<MeterTypeRow> byMeterType;
        private List<DayPoint> dailyDynamics;
        private List<String> problems;
        private TopBlock top;
    }

    /** KPI — блок ключевых показателей */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KpiBlock {
        private long totalPlans;          // Всего планов
        private long completedPlans;      // Выполнено
        private long pendingPlans;        // В работе
        private double completionRate;    // % выполнения
        private long totalMasters;        // Активных мастеров
        private long totalRegions;        // РЭС
        private double avgPerMaster;      // Среднее на мастера
        private long completedToday;      // Выполнено сегодня
        private long completedThisWeek;   // За неделю
        private long completedThisMonth;  // За месяц
    }

    /** Строка по РЭС */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegionRow {
        private String region;
        private long totalPlans;
        private long completed;
        private long pending;
        private double rate;        // % выполнения
        private String status;      // GREEN / YELLOW / RED
    }

    /** Строка по мастеру */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MasterRow {
        private String masterName;
        private String region;
        private long completed;
        private long pending;
        private long total;
        private double rate;
        private String lastActivity; // дата последней задачи
    }

    /** Строка по типу счётчика */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MeterTypeRow {
        private String meterType;
        private long count;
        private double percent;
    }

    /** Точка динамики по дням */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DayPoint {
        private String date;
        private long count;
    }

    /** ТОП / АНТИТОП блок */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopBlock {
        private MasterRow topMaster;
        private MasterRow bottomMaster;
        private RegionRow topRegion;
        private RegionRow bottomRegion;
    }
}
