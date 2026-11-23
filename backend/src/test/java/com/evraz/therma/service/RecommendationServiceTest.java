package com.evraz.therma.service;

import com.evraz.therma.detector.TelemetryDeviationDetector;
import com.evraz.therma.dto.*;
import com.evraz.therma.engine.RuleEngine;
import com.evraz.therma.explanation.ExplanationGenerator;
import com.evraz.therma.guard.DataSufficiencyGuard;
import com.evraz.therma.mapper.TelemetryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private TelemetryMapper telemetryMapper;
    @Mock private TelemetryDeviationDetector deviationDetector;
    @Mock private RuleEngine ruleEngine;
    @Mock private ExplanationGenerator explanationGenerator;
    @Mock private DataSufficiencyGuard dataSufficiencyGuard;
    @Mock private TelemetryHistoryProvider historyProvider;

    @InjectMocks
    private RecommendationService service;

    @Test
    void shouldBlockRuleEvaluationAndClearHistoryWhenInsufficientData() {
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());

        when(dataSufficiencyGuard.checkAndRecord("VN-1", record.getRecordedAt(), null))
            .thenReturn(SystemStatus.INSUFFICIENT_DATA);
        when(explanationGenerator.generate(any(), eq(record)))
            .thenReturn(List.of("Нет свежих данных"));

        List<Recommendation> result = service.processTelemetry(record);

        assertEquals(1, result.size());
        assertEquals("R7_INSUFFICIENT_DATA", result.getFirst().ruleCode());
        assertEquals(List.of("Нет свежих данных"), result.getFirst().reasoning());

        verify(historyProvider).clear("VN-1");
        verifyNoInteractions(deviationDetector, ruleEngine);
    }

    @Test
    void shouldRecordHistoryEvaluateRulesAndAttachReasoning() {
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1395.0);

        Recommendation raw = new Recommendation(
            null, "VN-1", record.getRecordedAt(), "R1_DOME_GROWING",
            "Рост купола", Priority.WARNING, RecommendationStatus.PENDING,
            new ExpectedEffect(-60.0, 0.0), List.of(), "air_gas_ratio", 1.02, 1.10
        );

        when(dataSufficiencyGuard.checkAndRecord("VN-1", record.getRecordedAt(), 1395.0))
            .thenReturn(SystemStatus.ACTIVE);
        when(historyProvider.getDomeTempHistory(eq("VN-1"), any())).thenReturn(List.of());
        when(historyProvider.getWasteGasTempHistory(eq("VN-1"), any())).thenReturn(List.of());
        when(deviationDetector.detect(record)).thenReturn(List.of());
        when(ruleEngine.evaluate(eq(record), any(), any(), any())).thenReturn(List.of(raw));
        when(explanationGenerator.generate(raw, record)).thenReturn(List.of("domeTemp > 1390.0 °C"));

        List<Recommendation> result = service.processTelemetry(record);

        assertEquals(1, result.size());
        assertEquals(List.of("domeTemp > 1390.0 °C"), result.getFirst().reasoning());
        verify(historyProvider).record(record);
    }
}
