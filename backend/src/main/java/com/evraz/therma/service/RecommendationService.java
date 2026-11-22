package com.evraz.therma.service;

import com.evraz.therma.detector.TelemetryDeviationDetector;
import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.DeviationEvent;
import com.evraz.therma.dto.Priority;
import com.evraz.therma.dto.Recommendation;
import com.evraz.therma.dto.RecommendationStatus;
import com.evraz.therma.dto.RuleCode;
import com.evraz.therma.dto.SystemStatus;
import com.evraz.therma.dto.TelemetryPoint;
import com.evraz.therma.dto.TelemetryRecord;
import com.evraz.therma.engine.RuleEngine;
import com.evraz.therma.explanation.ExplanationGenerator;
import com.evraz.therma.guard.DataSufficiencyGuard;
import com.evraz.therma.mapper.TelemetryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final Duration TREND_WINDOW = Duration.ofMinutes(15);

    private final TelemetryMapper telemetryMapper;
    private final TelemetryDeviationDetector deviationDetector;
    private final RuleEngine ruleEngine;
    private final ExplanationGenerator explanationGenerator;
    private final DataSufficiencyGuard dataSufficiencyGuard;
    private final TelemetryHistoryProvider historyProvider;

    public RecommendationService(
        TelemetryMapper telemetryMapper,
        TelemetryDeviationDetector deviationDetector,
        RuleEngine ruleEngine,
        ExplanationGenerator explanationGenerator,
        DataSufficiencyGuard dataSufficiencyGuard,
        TelemetryHistoryProvider historyProvider) {
        this.telemetryMapper = telemetryMapper;
        this.deviationDetector = deviationDetector;
        this.ruleEngine = ruleEngine;
        this.explanationGenerator = explanationGenerator;
        this.dataSufficiencyGuard = dataSufficiencyGuard;
        this.historyProvider = historyProvider;
    }

    public List<Recommendation> processTelemetry(TelemetryPoint telemetryPoint) {
        TelemetryRecord record = telemetryMapper.toRecord(telemetryPoint);
        return processTelemetry(record);
    }

    public List<Recommendation> processTelemetry(TelemetryRecord record) {
        if (record == null) {
            return List.of();
        }

        // Контроль свежести и достоверности входящего пакета
        SystemStatus status = dataSufficiencyGuard.checkAndRecord(
            record.getCowperId(),
            record.getRecordedAt(),
            record.getDomeTemp()
        );

        if (status == SystemStatus.INSUFFICIENT_DATA) {
            // Сбрасываем старый хвост буфера, чтобы после разрыва связи не считался ложный тренд
            historyProvider.clear(record.getCowperId());

            Recommendation r7 = new Recommendation();
            r7.setCowperId(record.getCowperId());
            r7.setCreatedAt(Instant.now());
            r7.setRuleCode(RuleCode.R7_INSUFFICIENT_DATA.name());
            r7.setPriority(Priority.WARNING);
            r7.setStatus(RecommendationStatus.PENDING);
            r7.setMessage("Недостаточно данных для формирования рекомендации");
            r7.setReasoning(explanationGenerator.generate(r7, record));
            return List.of(r7);
        }

        // Сохраняем валидную точку в окно трендов и извлекаем историю по текущему ВН
        historyProvider.record(record);
        List<DataPoint> domeHistory = historyProvider.getDomeTempHistory(record.getCowperId(), TREND_WINDOW);
        List<DataPoint> wasteHistory = historyProvider.getWasteGasTempHistory(record.getCowperId(), TREND_WINDOW);

        // Детектирование пороговых отклонений и расчёт рекомендаций
        List<DeviationEvent> deviationEvents = deviationDetector.detect(record);
        List<Recommendation> recommendations = ruleEngine.evaluate(
            record, deviationEvents, domeHistory, wasteHistory
        );

        // Формирование адресных объяснений
        for (Recommendation recommendation : recommendations) {
            recommendation.setReasoning(explanationGenerator.generate(recommendation, record));
        }

        return recommendations;
    }

    public SystemStatus getSystemStatus() {
        return dataSufficiencyGuard.getSystemStatus();
    }

    public SystemStatus getCowperStatus(String cowperId) {
        return dataSufficiencyGuard.getCowperStatus(cowperId);
    }

    public void saveRecommendations(List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        for (Recommendation rec : recommendations) {
            log.info("Сформирована рекомендация: rule={}, priority={}, cowperId={}, target={} [{}..{}]",
                rec.getRuleCode(),
                rec.getPriority(),
                rec.getCowperId(),
                rec.getTargetParameter(),
                rec.getSuggestedValueMin(),
                rec.getSuggestedValueMax());
        }
    }
}
