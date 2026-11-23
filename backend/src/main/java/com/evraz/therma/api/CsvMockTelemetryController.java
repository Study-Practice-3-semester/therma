package com.evraz.therma.api;

import com.evraz.therma.dto.Cowper;
import com.evraz.therma.dto.Phase;
import com.evraz.therma.dto.Recommendation;
import com.evraz.therma.dto.TelemetryPoint;
import com.evraz.therma.service.RecommendationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Временный контроллер для фронта
 */

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CsvMockTelemetryController {
    private static final DateTimeFormatter CSV_TS_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");
    private final RecommendationService recommendationService;

    private final List<TelemetryPoint> allPoints = new ArrayList<>();
    private final AtomicInteger cursor = new AtomicInteger(120*4); // старт со 2-го часа, чтобы были данные

    private Phase mapPhase(String raw) {
        return switch (raw) {
            case "gas" -> Phase.HEATING;
            case "blast" -> Phase.BLOWING;
            case "changeover" -> Phase.CHANGING;
            case "reserve" -> Phase.RESERVE;
            default -> null;
        };
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Double.parseDouble(raw.trim());
    }

    @PostConstruct
    public void loadCsv() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/hot_blast_stove_synthetic.csv");

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();

            if (header == null) {
                return;
            }

            Instant serverStart = Instant.now();
            Instant csvStartAnchor = LocalDateTime.parse("2026-03-12 10:00", CSV_TS_FORMAT).toInstant(ZoneOffset.UTC);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] c = line.split(",", -1);
                Instant rawTs = LocalDateTime.parse(c[0].trim(), CSV_TS_FORMAT).toInstant(ZoneOffset.UTC);
                Instant ts = serverStart.plus(Duration.between(csvStartAnchor, rawTs));
                String stoveId = c[1].trim();
                Phase phase = mapPhase(c[2].trim());

                allPoints.add(new TelemetryPoint(
                    stoveId, ts, phase,
                    parseDouble(c[3]),  // blast_flow_m3min
                    parseDouble(c[4]),  // cold_blast_temp_C
                    parseDouble(c[5]),  // hot_blast_setpoint_C
                    parseDouble(c[6]),  // hot_blast_temp_C
                    parseDouble(c[7]),  // mixing_cold_share_pct
                    parseDouble(c[8]),  // stove_outlet_temp_C
                    parseDouble(c[9]),  // mixed_gas_flow_m3h
                    null,               // bfGasFlow
                    null,               // natGasFlow
                    parseDouble(c[10]), // gas_calorific_MJm3
                    parseDouble(c[11]), // enrichment_ng_pct
                    parseDouble(c[18]), // gas_pressure_kPa
                    parseDouble(c[12]), // air_flow_m3h
                    parseDouble(c[13]), // air_gas_ratio
                    parseDouble(c[14]), // dome_temp_C
                    parseDouble(c[15]), // waste_gas_temp_C
                    parseDouble(c[16]), // flue_O2_pct
                    parseDouble(c[17])  // flue_CO_ppm
                ));
            }
        }
    }

    @GetMapping("/cowpers")
    public List<Cowper> getCowpers() {
        return List.of(
            new Cowper("VN-1", "Воздухонагреватель №1", "BLOWING"),
            new Cowper("VN-2", "Воздухонагреватель №2", "HEATING"),
            new Cowper("VN-3", "Воздухонагреватель №3", "HEATING"),
            new Cowper("VN-4", "Воздухонагреватель №4", "RESERVE")
        );
    }

    // Отдаёт срез точек до текущего курсора (для первичной отрисовки графиков)
    @GetMapping("/telemetry")
    public List<TelemetryPoint> getHistory(
        @RequestParam(required = false) String cowperId,
        @RequestParam(defaultValue = "240") int limitMinutes
    ) {
        int end = Math.min(cursor.get(), allPoints.size());
        int start = Math.max(0, end - limitMinutes * 4);
        return allPoints.subList(start, end).stream()
            .filter(p -> cowperId == null || cowperId.isBlank() || cowperId.equals(p.cowperId()))
            .toList();
    }

    // Дёргается фронтом по таймеру (или кнопкой "Следующий тик"), пока не прикручен STOMP
    @PostMapping("/telemetry/next-tick")
    public Map<String, Object> nextTick() {
        int idx = cursor.getAndUpdate(i -> (i + 4 >= allPoints.size()) ? 4 : i + 4);
        int end = Math.min(idx + 4, allPoints.size());
        List<TelemetryPoint> tickBatch = allPoints.subList(idx, end);

        List<Recommendation> newRecs = new ArrayList<>();
        for (TelemetryPoint point : tickBatch) {
            newRecs.addAll(recommendationService.processTelemetry(point));
        }

        return Map.of(
            "points", tickBatch,
            "recommendations", newRecs
        );
    }
}
