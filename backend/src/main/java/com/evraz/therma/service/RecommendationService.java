package com.evraz.therma.service;

import java.time.Instant;
import java.util.List;

import com.evraz.therma.detector.TelemetryDeviationDetector;
import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.dto.*;
import com.evraz.therma.engine.RuleEngine;
import com.evraz.therma.explanation.ExplanationGenerator;
import com.evraz.therma.guard.DataSufficiencyGuard;
import com.evraz.therma.mapper.TelemetryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private final TelemetryMapper telemetryMapper;
    private final TelemetryDeviationDetector deviationDetector;
    private final RuleEngine ruleEngine;
    private final ExplanationGenerator explanationGenerator;
    private final DataSufficiencyGuard dataSufficiencyGuard;

    public RecommendationService(
        TelemetryMapper telemetryMapper,
        TelemetryDeviationDetector deviationDetector,
        RuleEngine ruleEngine,
        ExplanationGenerator explanationGenerator,
        DataSufficiencyGuard dataSufficiencyGuard) {
        this.telemetryMapper = telemetryMapper;
        this.deviationDetector = deviationDetector;
        this.ruleEngine = ruleEngine;
        this.explanationGenerator = explanationGenerator;
        this.dataSufficiencyGuard = dataSufficiencyGuard;
    }

    public List<Recommendation> processTelemetryFromRest(TelemetryPoint telemetryPoint) {
        TelemetryRecord record = telemetryMapper.fromRest(telemetryPoint);
        return processTelemetry(record);
    }

    public List<Recommendation> processTelemetryFromStomp(TelemetryMessage telemetryMessage) {
        TelemetryRecord record = telemetryMapper.fromStomp(telemetryMessage);
        return processTelemetry(record);
    }


    private List<Recommendation> processTelemetry(TelemetryRecord record) {
        // Проверка достаточности данных
        SystemStatus status = dataSufficiencyGuard.check(
            record.getRecordedAt(),
            record.getDomeTemp()
        );

        if (status == SystemStatus.INSUFFICIENT_DATA) {
            Recommendation r7 = new Recommendation();
            r7.setCowperId(record.getCowperId());
            r7.setCreatedAt(Instant.now());
            r7.setRuleCode(RuleCode.R7_INSUFFICIENT_DATA.name());
            r7.setPriority(Priority.WARNING);
            r7.setStatus(RecommendationStatus.PENDING);
            r7.setMessage("Недостаточно данных для формирования рекомендации");
            return List.of(r7);
        }

        // Детектирование отклонений
        List<DeviationEvent> deviationEvents = deviationDetector.detect(record);

        // История для трендовых правил (пока пустая — в реальном проекте из БД)
        List<TrendAnalyzer.DataPoint> domeHistory = List.of();
        List<TrendAnalyzer.DataPoint> wasteHistory = List.of();

        // Формирование рекомендаций
        List<Recommendation> recommendations = ruleEngine.evaluate(
            record, deviationEvents, domeHistory, wasteHistory);

        // Генерация объяснений
        for (Recommendation recommendation : recommendations) {
            List<String> explanations = explanationGenerator.generate(recommendation, record);
            recommendation.setReasoning(explanations);
        }

        return recommendations;
    }

    public SystemStatus getSystemStatus() {
        return dataSufficiencyGuard.check(null, null);
    }

    public void saveRecommendations(List<Recommendation> recommendations) {
        for (Recommendation rec : recommendations) {
            System.out.println("Сохранена рекомендация: " + rec.getRuleCode() + " для ВН " + rec.getCowperId());
        }
    }
}
