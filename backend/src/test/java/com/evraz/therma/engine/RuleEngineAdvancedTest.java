package com.evraz.therma.engine;

import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineAdvancedTest {

    private RuleEngine ruleEngine;
    private CombustionCalculator calc;
    private TrendAnalyzer trendAnalyzer;

    @BeforeEach
    void setUp() {
        calc = new CombustionCalculator();
        trendAnalyzer = new TrendAnalyzer();
        ruleEngine = new RuleEngine(calc, trendAnalyzer);
    }

    @Test
    void r1_shouldTriggerWhenDomeGrowingAbove1390() {
        // Дано: купол 1395 °C и растёт
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1395.0);
        record.setAirGasRatio(0.97);
        record.setEnrichmentNg(3.0);

        List<DataPoint> domeHistory = new ArrayList<>();
        Instant now = Instant.now();
        domeHistory.add(new DataPoint(now.minusSeconds(600), 1380.0));
        domeHistory.add(new DataPoint(now.minusSeconds(300), 1388.0));
        domeHistory.add(new DataPoint(now, 1395.0));

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), domeHistory, List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R1_DOME_GROWING")));
    }

    @Test
    void r2_shouldTriggerWhenWasteGasTrendingTo400() {
        // Дано: дымы 385 °C и растут к 400
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setWasteGasTemp(385.0);

        List<DataPoint> wasteHistory = new ArrayList<>();
        Instant now = Instant.now();
        wasteHistory.add(new DataPoint(now.minusSeconds(600), 360.0));
        wasteHistory.add(new DataPoint(now.minusSeconds(300), 375.0));
        wasteHistory.add(new DataPoint(now, 385.0));

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), wasteHistory);

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R2_WASTE_TREND")));
    }

    @Test
    void r3_shouldTriggerOnHighCO() {
        // Дано: CO > 1000 ppm
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setFlueCO(1500.0);
        record.setFlueO2(0.2);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R3_INCOMPLETE_COMBUSTION")));
    }

    @Test
    void r3_shouldTriggerOnLowO2() {
        // Дано: O2 < 0.3%
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setFlueO2(0.2);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R3_INCOMPLETE_COMBUSTION")));
    }

    @Test
    void r4_shouldTriggerWhenQDroppedAndDomeLow() {
        // Дано: Q_см упала на > 5%, купол ниже цели
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setGasCalorific(3.8); // упала с 4.2 на ~10%
        record.setDomeTemp(1300.0);  // ниже цели 1350
        record.setBlastFlow(4000.0);
        record.setHotBlastSetpoint(1150.0);
        record.setColdBlastTemp(180.0);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R4_CALORIFIC_DROP")));
    }

    @Test
    void r5_shouldTriggerWhenBlastUnderheatedWithClosedMixer() {
        // Дано: T_гд ниже уставки на 30 °C, смеситель закрыт
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setHotBlastTemp(1120.0);
        record.setHotBlastSetpoint(1150.0);
        record.setMixingColdShare(0.0); // смеситель закрыт
        record.setBlastFlow(4000.0);
        record.setColdBlastTemp(180.0);
        record.setGasCalorific(4.2);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R5_BLAST_UNDERHEAT")));
    }

    @Test
    void r6_shouldTriggerOnHighO2WithLowDome() {
        // Дано: O2 > 3%, купол ниже предела
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setFlueO2(3.5);
        record.setDomeTemp(1300.0); // ниже 1350
        record.setAirGasRatio(1.1);
        record.setEnrichmentNg(3.0);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("R6_O2_EXCESS")));
    }

    @Test
    void r7_shouldTriggerOnSpike() {
        // Дано: скачок купола > 50 °C/мин
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1350.0);

        List<DataPoint> domeHistory = new ArrayList<>();
        Instant now = Instant.now();
        domeHistory.add(new DataPoint(now.minusSeconds(60), 1300.0));
        domeHistory.add(new DataPoint(now, 1351.0)); // скачок 50 °C за минуту

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), domeHistory, List.of());

        // Тогда: R7 блокирует всё
        assertEquals(1, recs.size());
        assertEquals("R7_INSUFFICIENT_DATA", recs.get(0).getRuleCode());
    }

    @Test
    void r7_shouldTriggerOnFrozenReadings() {
        // Дано: купол не меняется 10+ минут
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1350.0);

        List<DataPoint> domeHistory = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 12; i >= 0; i--) {
            domeHistory.add(new DataPoint(now.minusSeconds(i * 60), 1350.0));
        }

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), domeHistory, List.of());

        // Тогда
        assertEquals(1, recs.size());
        assertEquals("R7_INSUFFICIENT_DATA", recs.get(0).getRuleCode());
    }

    @Test
    void safetyBlock_shouldTriggerOnWasteGasAbove450() {
        // Дано: дымы > 450 °C
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setWasteGasTemp(460.0);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("DHN_WASTE_OVERHEAT")));
    }

    @Test
    void safetyBlock_shouldTriggerOnDomeAbove1400() {
        // Дано: купол > 1400 °C
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1410.0);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда
        assertTrue(recs.stream().anyMatch(r -> r.getRuleCode().equals("DHN_DOME_OVERHEAT")));
    }

    @Test
    void noRecommendationsForNormalTelemetry() {
        // Дано: все параметры в норме
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1350.0);
        record.setWasteGasTemp(300.0);
        record.setFlueO2(1.5);
        record.setFlueCO(100.0);
        record.setGasCalorific(4.2);
        record.setHotBlastTemp(1150.0);
        record.setHotBlastSetpoint(1150.0);
        record.setMixingColdShare(5.0);
        record.setAirGasRatio(1.0);
        record.setEnrichmentNg(3.0);

        // Когда
        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), List.of(), List.of());

        // Тогда: рекомендаций нет
        assertTrue(recs.isEmpty());
    }
}
