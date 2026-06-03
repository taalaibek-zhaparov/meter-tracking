package com.metertracking.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertracking.dto.OneCMeterReadingDTO;
import com.metertracking.entity.CompletedTask;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OneCHttpService {

    @Value("${onec.server}")
    private String server;

    @Value("${onec.bases}")
    private String basesConfig;

    @Value("${onec.user}")
    private String username;

    @Value("${onec.password}")
    private String password;

    private final Map<String, String> prefixToBase = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ── Инициализация ─────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        String[] tokens = basesConfig.split(",");
        StringBuilder buffer = new StringBuilder();

        for (String token : tokens) {
            token = token.trim();
            if (token.contains(":")) {
                String[] parts = token.split(":");
                if (!buffer.isEmpty()) buffer.append(",").append(parts[0]);
                else buffer.append(parts[0]);
                String base = parts[1].trim();
                for (String prefix : buffer.toString().split(",")) {
                    prefixToBase.put(prefix.trim(), base);
                }
                buffer.setLength(0);
            } else {
                if (!buffer.isEmpty()) buffer.append(",");
                buffer.append(token);
            }
        }
        log.info("1С базы: {}", prefixToBase);
    }

    private String resolveBase(String account) {
        if (account == null || account.length() < 2) return null;
        return prefixToBase.get(account.substring(0, 2));
    }

    // ── Получение данных счётчика из 1С ──────────────────────────────

    @Data
    private static class OneCResponse {
        @JsonProperty("res")       private String res;
        @JsonProperty("lcheet")    private String lcheet;
        @JsonProperty("fio")       private String fio;
        @JsonProperty("adres")     private String adres;
        @JsonProperty("tp")        private String tp;
        @JsonProperty("counter")   private String counter;
        @JsonProperty("counterNo") private String counterNo;
        @JsonProperty("pkz")       private Double pkz;
        @JsonProperty("datepkz")   private String datepkz;
        @JsonProperty("dolg")      private Double dolg;
    }

    public Optional<OneCMeterReadingDTO> getLastMeterInfo(String licevoy) {
        try {
            log.info("HTTP 1С: запрос для {}", licevoy);

            String base = resolveBase(licevoy);
            if (base == null) {
                log.warn("Не найдена база для {}", licevoy);
                return Optional.empty();
            }

            String url = "http://" + server + "/" + base + "/hs/tb";

            String raw = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeaders(h -> h.setBasicAuth(username, password))
                    .build()
                    .get()
                    .uri("/Lcheet/{licevoy}", licevoy)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (raw != null && raw.startsWith("\uFEFF")) raw = raw.substring(1);
            log.info("HTTP response: {}", raw);

            if (raw == null || !raw.trim().startsWith("{")) {
                log.error("Получен не JSON: {}", raw);
                return Optional.empty();
            }

            OneCResponse response = objectMapper.readValue(raw, OneCResponse.class);

            OneCMeterReadingDTO dto = new OneCMeterReadingDTO();
            dto.setAccount(response.getLcheet() != null ? response.getLcheet() : licevoy);
            dto.setRegion(response.getRes());
            dto.setFio(response.getFio());
            dto.setAdres(response.getAdres());
            dto.setTp(response.getTp());
            dto.setMeterType(response.getCounter());
            dto.setMeterNumber(response.getCounterNo());
            dto.setReading(response.getPkz());

            if (response.getDatepkz() != null && !response.getDatepkz().isBlank()) {
                try {
                    dto.setDate(LocalDate.parse(response.getDatepkz().trim(),
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                } catch (DateTimeParseException e) {
                    log.warn("Ошибка парсинга даты {} для {}", response.getDatepkz(), licevoy);
                    dto.setDate(null);
                }
            }

            log.info("Успешно: {} → счётчик {}", licevoy, dto.getMeterNumber());
            return Optional.of(dto);

        } catch (WebClientResponseException e) {
            log.error("HTTP ошибка {} для {}: {}", e.getStatusCode(), licevoy, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Ошибка запроса 1С для {}: {}", licevoy, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Проверка доступности 1С ───────────────────────────────────────

    public boolean isAvailable() {
        try {
            String url = "http://" + server + "/jres/hs/tb/Lcheet/00000000000";
            String raw = WebClient.builder()
                    .defaultHeaders(h -> h.setBasicAuth(username, password))
                    .build()
                    .get().uri(url).retrieve().bodyToMono(String.class).block();
            if (raw != null && raw.startsWith("\uFEFF")) raw = raw.substring(1);
            return raw != null;
        } catch (Exception e) {
            log.warn("1С недоступна: {}", e.getMessage());
            return false;
        }
    }

    // ── Отправка выполненной задачи в 1С ─────────────────────────────

    public boolean sendCompletedTask(CompletedTask task) {
        try {
            String url = "http://" + server + "/proba831/hs/meters/replacemeter/";

            String docNumber = task.getDocumentNumber() != null ? task.getDocumentNumber() : "";
            String dateDoc   = formatDateDoc(task.getCreatedAt(), task.getData());
            String amper     = buildAmperString(task.getAmperage());
            int    znch      = task.getZnch()   != null ? task.getZnch()   : 0;
            int    faza      = task.getPhases() != null ? task.getPhases() : 0;
            int meterCode = task.getMeterCode() != null ? task.getMeterCode() : 0;
            // Старый счётчик (для поиска в 1С) — берём из плана
            String oldMeterType = (task.getOldMeterType() != null && !task.getOldMeterType().isBlank())
                    ? task.getOldMeterType()
                    : nvl(task.getTip());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("DocNumber",      docNumber);
            body.put("lcheet",         nvl(task.getLicevoy()));
            body.put("ElMonter",       task.getUser() != null ? task.getUser().getUsername() : "");
            body.put("Meter",          oldMeterType);        // ← старый тип: "DTS27"
            body.put("MeterCode", meterCode);
            body.put("MeterNumber",    nvl(task.getOldNomerSchetchika())); // старый номер
            body.put("DateDoc",        dateDoc);
            body.put("PKZ",            task.getOldPokazaniya() != null ? task.getOldPokazaniya() : 0.0);
            body.put("NewMeter",       nvl(task.getTip()));  // ← новый тип: "HXF-300 100В KUK"
            body.put("NewMeterNumber", nvl(task.getNomerSchetchika()));    // новый номер
            body.put("NewPKZ",         task.getNewPokazaniya() != null ? task.getNewPokazaniya() : 0.0);
            body.put("KTT",     1);
            body.put("Amper",          amper);
            body.put("Faza",           faza);
            body.put("ZNCH",           znch);
            body.put("Plomb11",        nvl(task.getPlombaGos()));
            body.put("Plomb12",        nvl(task.getNomerPlomby()));
            body.put("Plomb13",        nvl(task.getNaKryshke()));
            body.put("Plomb14",        nvl(task.getNaYashike()));
            body.put("DocType",        nvl(task.getDocumentType()));

            String json = objectMapper.writeValueAsString(body);
            log.info("Отправка в 1С [{}]: {}", task.getLicevoy(), json);

            String response = WebClient.builder()
                    .defaultHeaders(h -> h.setBasicAuth(username, password))
                    .build()
                    .post().uri(url)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(json)
                    .retrieve().bodyToMono(String.class).block();

            log.info("Ответ 1С для {}: {}", task.getLicevoy(), response);
            return true;

        } catch (WebClientResponseException e) {
            log.error("HTTP ошибка 1С {} для {}: body={}",
                    e.getStatusCode(), task.getLicevoy(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Ошибка отправки в 1С для {}: {}", task.getLicevoy(), e.getMessage(), e);
            return false;
        }
    }

    // ── Приватные хелперы ─────────────────────────────────────────────

    /**
     * DateDoc: используем createdAt (время выполнения задачи).
     * Формат: "2026-04-08T14:14:27"
     */
    private String formatDateDoc(LocalDateTime createdAt, LocalDate planDate) {
        if (createdAt != null) return createdAt.format(DATE_TIME_FMT);
        if (planDate  != null) return planDate.atStartOfDay().format(DATE_TIME_FMT);
        return LocalDateTime.now().format(DATE_TIME_FMT);
    }

    /**
     * Строит строку тока для поля Amper.
     * Из числа 5 → "5А". Если нужен полный формат "5(80)А" —
     * добавьте поле ampSpec в CompletedTask и передавайте его.
     */
    private String buildAmperString(Integer amperage) {
        if (amperage != null) return amperage + "А";
        return "";
    }

    /** Null-safe строка: null → "" */
    private static String nvl(String s) {
        return s != null ? s : "";
    }
}