package com.evraz.therma.service;

import com.evraz.therma.detector.TelemetryDeviationDetector;
import com.evraz.therma.dto.*;
import com.evraz.therma.engine.RuleEngine;
import com.evraz.therma.explanation.ExplanationGenerator;
import com.evraz.therma.guard.DataSufficiencyGuard;
import com.evraz.therma.mapper.TelemetryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервисный слой, объединяющий все модули бизнес-логики.
 * Координирует работу детектора, движка правил и генератора объяснений.
 */
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

    /**
     * Обработка телеметрии из REST API.
     */
    public List<Recommendation> processTelemetryFromRest(TelemetryPoint telemetryPoint) {
        TelemetryRecord record = telemetryMapper.fromRest(telemetryPoint);
        return processTelemetry(record);
    }

    /**
     * Обработка телеметрии из STOMP WebSocket.
     */
    public List<Recommendation> processTelemetryFromStomp(TelemetryMessage telemetryMessage) {
        TelemetryRecord record = telemetryMapper.fromStomp(telemetryMessage);
        return processTelemetry(record);
    }

    /**
     * Основная логика обработки телеметрии.
     */
    private List<Recommendation> processTelemetry(TelemetryRecord record) {
        // Проверка достаточности данных (п. 5.5)
        SystemStatus status = dataSufficiencyGuard.check(
            record.getRecordedAt(),
            record.getDomeTemp()
        );

        if (status == SystemStatus.INSUFFICIENT_DATA) {
            // Возвращаем пустой список — рекомендация не выдаётся
            return new ArrayList<>();
        }

        // Детектирование отклонений (п. 5.2)
        List<DeviationEvent> deviationEvents = deviationDetector.detect(record);

        // Формирование рекомендаций (п. 5.3)
        List<Recommendation> recommendations = ruleEngine.evaluate(record, deviationEvents);

        // Генерация объяснений (п. 5.4)
        for (Recommendation recommendation : recommendations) {
            List<String> explanations = explanationGenerator.generate(recommendation, record);
            recommendation.setReasoning(explanations);
        }

        return recommendations;
    }

    /**
     * Получение текущего статуса системы.
     */
    public SystemStatus getSystemStatus() {
        // Возвращаем последний известный статус
        return dataSufficiencyGuard.check(null, null);
    }
    /**
     * Сохранение рекомендаций в хранилище (для контроллера).
     */
    public void saveRecommendations(List<Recommendation> recommendations) {
        // В реальном проекте здесь будет сохранение в БД через репозиторий
        // Пока просто логируем
        for (Recommendation rec : recommendations) {
            System.out.println("Сохранена рекомендация: " + rec.getRuleCode() + " для ВН " + rec.getCowperId());
        }
    }
}
