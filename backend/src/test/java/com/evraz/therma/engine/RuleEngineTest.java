package com.evraz.therma.engine;

import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        CombustionCalculator calc = new CombustionCalculator();
        TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
        ruleEngine = new RuleEngine(calc, trendAnalyzer);
    }

    @Test
    void shouldGenerateRecommendationForDomeOverheat() {
        // Дано
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setDomeTemp(1400.0);

        DeviationEvent event = new DeviationEvent();
        event.setRuleCode(RuleCode.DHN_DOME_OVERHEAT);
        event.setPriority(Priority.CRITICAL);

        List<DeviationEvent> events = List.of(event);

        // Когда
        List<Recommendation> recommendations = ruleEngine.evaluate(record, events);

        // Тогда
        assertFalse(recommendations.isEmpty());
        assertEquals(1, recommendations.size());
        assertEquals("DHN_DOME_OVERHEAT", recommendations.get(0).getRuleCode());
        assertEquals(Priority.CRITICAL, recommendations.get(0).getPriority());
        assertEquals(RecommendationStatus.PENDING, recommendations.get(0).getStatus());
        assertNotNull(recommendations.get(0).getExpectedEffect());
    }

    @Test
    void shouldReturnEmptyForNullRecord() {
        // Когда
        List<Recommendation> recommendations = ruleEngine.evaluate(null, Collections.emptyList());

        // Тогда
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullEvents() {
        // Дано
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");

        // Когда
        List<Recommendation> recommendations = ruleEngine.evaluate(record, null);

        // Тогда
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyEvents() {
        // Дано
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");

        // Когда
        List<Recommendation> recommendations = ruleEngine.evaluate(record, Collections.emptyList());

        // Тогда
        assertTrue(recommendations.isEmpty());
    }
}
