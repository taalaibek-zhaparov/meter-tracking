package com.metertracking.service;

import com.metertracking.entity.CompletedTask;
import com.metertracking.entity.Plan;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    /**
     * Экспорт планов — добавлены колонки ФИО и Адрес.
     */
    public byte[] exportPlans(List<Plan> plans) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Планы");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle   = createDataStyle(workbook);

            // ДОБАВЛЕНЫ: ФИО и Адрес
            String[] headers = {
                    "ID", "Мастер", "Регион", "ТП", "Лицевой счёт",
                    "ФИО", "Адрес",
                    "Тип счётчика", "Номер счётчика (старый)", "Показания (старый)", "Дата", "Статус"
            };
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            for (Plan plan : plans) {
                Row row = sheet.createRow(rowNum++);
                applyDataStyle(row, headers.length, dataStyle);

                row.getCell(0).setCellValue(plan.getId());
                row.getCell(1).setCellValue(plan.getUser() != null ? plan.getUser().getUsername() : "");
                row.getCell(2).setCellValue(nullSafe(plan.getRegion()));
                row.getCell(3).setCellValue(nullSafe(plan.getTp()));
                row.getCell(4).setCellValue(nullSafe(plan.getLicevoy()));
                // НОВЫЕ ПОЛЯ
                row.getCell(5).setCellValue(nullSafe(plan.getFio()));
                row.getCell(6).setCellValue(nullSafe(plan.getAdres()));
                // Остальные поля
                row.getCell(7).setCellValue(nullSafe(plan.getTip()));
                row.getCell(8).setCellValue(nullSafe(plan.getNomerSchetchika()));
                row.getCell(9).setCellValue(plan.getPokazaniya() != null ? plan.getPokazaniya() : 0);
                row.getCell(10).setCellValue(plan.getData() != null ? plan.getData().toString() : "");
                row.getCell(11).setCellValue(Boolean.TRUE.equals(plan.getCompleted()) ? "Выполнено" : "В ожидании");
            }

            autoSizeColumns(sheet, headers.length);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Экспорт выполненных задач — добавлены колонки ФИО и Адрес.
     */
    public byte[] exportCompletedTasks(List<CompletedTask> tasks) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Выполненные задачи");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle   = createDataStyle(workbook);

            // ДОБАВЛЕНЫ: ФИО и Адрес
            String[] headers = {
                    "ID", "Мастер", "Регион", "ТП", "Лицевой счёт", "Тип счётчика",
                    "ФИО", "Адрес",
                    "Старый счётчик", "Показания (старый)",
                    "Новый счётчик", "Фазность", "Ампер", "Значность",
                    "Дата",
                    "Пломба гос.", "Одноразовая пломба", "На крышке", "На ящике",
                    "Номер SIM", "ICCID",
                    "Создано", "Обновлено"
            };
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            for (CompletedTask task : tasks) {
                Row row = sheet.createRow(rowNum++);
                applyDataStyle(row, headers.length, dataStyle);

                row.getCell(0).setCellValue(task.getId());
                row.getCell(1).setCellValue(task.getUser() != null ? task.getUser().getUsername() : "");
                row.getCell(2).setCellValue(nullSafe(task.getRegion()));
                row.getCell(3).setCellValue(nullSafe(task.getTp()));
                row.getCell(4).setCellValue(nullSafe(task.getLicevoy()));
                row.getCell(5).setCellValue(nullSafe(task.getTip()));
                // НОВЫЕ ПОЛЯ
                row.getCell(6).setCellValue(nullSafe(task.getFio()));
                row.getCell(7).setCellValue(nullSafe(task.getAdres()));

                // Старый счётчик
                String oldMeter = task.getOldNomerSchetchika() != null && !task.getOldNomerSchetchika().isBlank()
                        ? task.getOldNomerSchetchika()
                        : nullSafe(task.getNomerSchetchika());
                row.getCell(8).setCellValue(oldMeter);

                double oldPok = task.getOldPokazaniya() != null
                        ? task.getOldPokazaniya()
                        : (task.getPokazaniya() != null ? task.getPokazaniya() : 0);
                row.getCell(9).setCellValue(oldPok);

                row.getCell(10).setCellValue(nullSafe(task.getNomerSchetchika()));
                row.getCell(11).setCellValue(task.getPhases() != null ? task.getPhases() : 0);
                row.getCell(12).setCellValue(task.getAmperage() != null ? task.getAmperage() + " А" : "");
                row.getCell(13).setCellValue(task.getZnch() != null ? task.getZnch() : 0);      // Значность
                row.getCell(14).setCellValue(task.getData() != null ? task.getData().toString() : ""); // Дата ← было 13
                row.getCell(15).setCellValue(nullSafe(task.getPlombaGos()));
                row.getCell(16).setCellValue(nullSafe(task.getNomerPlomby()));
                row.getCell(17).setCellValue(nullSafe(task.getNaKryshke()));
                row.getCell(18).setCellValue(nullSafe(task.getNaYashike()));
                row.getCell(19).setCellValue(nullSafe(task.getNomerSimKarty()));
                row.getCell(20).setCellValue(nullSafe(task.getNomerIccid()));
                row.getCell(21).setCellValue(task.getCreatedAt() != null
                        ? task.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "");
                row.getCell(22).setCellValue(task.getUpdatedAt() != null
                        ? task.getUpdatedAt().toString().replace("T", " ").substring(0, 16) : "");
            }

            autoSizeColumns(sheet, headers.length);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ── СТИЛИ ──────────────────────────────────────────────

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void applyDataStyle(Row row, int count, CellStyle style) {
        for (int i = 0; i < count; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }

    private String nullSafe(String val) {
        return val != null ? val : "";
    }
}