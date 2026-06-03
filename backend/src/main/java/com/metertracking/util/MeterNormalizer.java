package com.metertracking.util;

import org.springframework.util.StringUtils;

/**
 * Утилита нормализации полей справочника счётчиков из 1С.
 *
 * <p>Правила:
 * <ul>
 *   <li><b>amp</b>: кириллическая «А» → латинская «A», убрать пробелы, upper-case.</li>
 *   <li><b>voltage</b>: убрать пробелы.</li>
 *   <li><b>name</b>: trim + схлопнуть двойные пробелы в один.</li>
 * </ul>
 */
public final class MeterNormalizer {

    private MeterNormalizer() {}

    /**
     * Нормализует значение тока (Ампер).
     * Примеры: "5(80)A", "100 A", "5(80)А" (кириллица) → "5(80)A", "100A".
     */
    public static String normalizeAmp(String raw) {
        if (!StringUtils.hasText(raw)) return raw;
        return raw
                .replace('\u0410', 'A')   // кириллическая заглавная А
                .replace('\u0430', 'A')   // кириллическая строчная а
                .replaceAll("\\s+", "")   // убрать все пробелы
                .toUpperCase();
    }

    /**
     * Нормализует напряжение.
     * Пример: "220 В" → "220В", "3х220/380 В" → "3х220/380В".
     */
    public static String normalizeVoltage(String raw) {
        if (!StringUtils.hasText(raw)) return raw;
        return raw.replaceAll("\\s+", "");
    }

    /**
     * Нормализует наименование модели.
     */
    public static String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) return raw;
        return raw.trim().replaceAll("\\s{2,}", " ");
    }
}