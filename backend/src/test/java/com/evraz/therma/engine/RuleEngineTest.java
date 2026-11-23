package com.evraz.therma.engine;

import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(new CombustionCalculator(), new TrendAnalyzer());
    }

    @Test
    void r7_shouldBlockAllOtherRulesOnSensorSpike() {
        TelemetryRecord record = baseHeatingRecord();
        record.setDomeTemp(1420.0); // одновременно критический перегрев

        Instant now = Instant.now();
        List<DataPoint> history = List.of(
            new DataPoint(now.minusSeconds(60), 1340.0),
            new DataPoint(now, 1420.0) // скачок +80 градусов
        );

        List<Recommendation> recs = ruleEngine.evaluate(record, List.of(), history, List.of());

        assertEquals(1, recs.size());
        assertEquals("R7_INSUFFICIENT_DATA", recs.getFirst().ruleCode());
        assertEquals("VN-1", recs.getFirst().cowperId());
    }

    @Test
    void safety_shouldTriggerOnLowGasPressure() {
        TelemetryRecord record = baseHeatingRecord();
        record.setGasPressure(4.2); // < 5.0 кПа

        List<Recommendation> recs = ruleEngine.evaluate(record, List.of());

        assertEquals(1, recs.size());
        assertEquals("SAFETY_GAS_PRESSURE", recs.getFirst().ruleCode());
        assertEquals(Priority.CRITICAL, recs.getFirst().priority());
        assertEquals(0.0, recs.getFirst().suggestedValueMax());
    }

    @Test
    void r1_r2_r3_r4_r6_shouldEvaluateCorrectlyInHeatingPhase() {
        Instant now = Instant.now();

        // R1: купол > 1390 и растёт -> рекомендация по air_gas_ratio, расход газа не меняется
        TelemetryRecord r1Rec = baseHeatingRecord();
        r1Rec.setDomeTemp(1395.0);
        List<DataPoint> domeRising = List.of(
            new DataPoint(now.minusSeconds(300), 1385.0),
            new DataPoint(now, 1395.0)
        );
        List<Recommendation> r1Out = ruleEngine.evaluate(r1Rec, List.of(), domeRising, List.of());
        assertTrue(r1Out.stream().anyMatch(r ->
            "R1_DOME_GROWING".equals(r.ruleCode())
                && "air_gas_ratio".equals(r.targetParameter())
                && r.expectedEffect().gasDelta() == 0.0));

        // R2: дымы > 380 и прогноз >= 400 -> снижение mixed_gas_flow
        TelemetryRecord r2Rec = baseHeatingRecord();
        r2Rec.setWasteGasTemp(388.0);
        List<DataPoint> wasteRising = List.of(
            new DataPoint(now.minusSeconds(300), 375.0),
            new DataPoint(now, 388.0)
        );
        List<Recommendation> r2Out = ruleEngine.evaluate(r2Rec, List.of(), List.of(), wasteRising);
        assertTrue(r2Out.stream().anyMatch(r ->
            "R2_WASTE_TREND".equals(r.ruleCode())
                && "mixed_gas_flow".equals(r.targetParameter())
                && r.suggestedValueMax() < 46000.0));

        // R3: химический недожог (CO > 1000 ppm)
        TelemetryRecord r3Rec = baseHeatingRecord();
        r3Rec.setFlueCO(1800.0);
        assertTrue(ruleEngine.evaluate(r3Rec, List.of()).stream()
            .anyMatch(r -> "R3_INCOMPLETE_COMBUSTION".equals(r.ruleCode())));

        // R4: падение теплоты сгорания Q_см > 5% при недогретом куполе
        TelemetryRecord r4Rec = baseHeatingRecord();
        r4Rec.setGasCalorific(3.8);
        r4Rec.setDomeTemp(1300.0);
        assertTrue(ruleEngine.evaluate(r4Rec, List.of()).stream()
            .anyMatch(r -> "R4_CALORIFIC_DROP".equals(r.ruleCode())));

        // R6: избыток O2 > 3% при недогретом куполе
        TelemetryRecord r6Rec = baseHeatingRecord();
        r6Rec.setFlueO2(3.4);
        r6Rec.setDomeTemp(1310.0);
        assertTrue(ruleEngine.evaluate(r6Rec, List.of()).stream()
            .anyMatch(r -> "R6_O2_EXCESS".equals(r.ruleCode())));
    }

    @Test
    void r5_shouldTriggerOnlyInBlowingPhaseWhenMixerClosed() {
        TelemetryRecord blowing = new TelemetryRecord();
        blowing.setCowperId("VN-3");
        blowing.setPhase(Phase.BLOWING);
        blowing.setHotBlastSetpoint(1180.0);
        blowing.setHotBlastTemp(1145.0); // недогрев 35 C > 25 C
        blowing.setMixingColdShare(0.0);
        blowing.setBlastFlow(4100.0);
        blowing.setColdBlastTemp(180.0);
        blowing.setGasCalorific(4.2);

        List<Recommendation> blowingRecs = ruleEngine.evaluate(blowing, List.of());
        assertEquals(1, blowingRecs.size());
        assertEquals("R5_BLAST_UNDERHEAT", blowingRecs.getFirst().ruleCode());

        // В фазе HEATING правило R5 срабатывать не должно
        blowing.setPhase(Phase.HEATING);
        assertTrue(ruleEngine.evaluate(blowing, List.of()).stream()
            .noneMatch(r -> "R5_BLAST_UNDERHEAT".equals(r.ruleCode())));
    }

    @Test
    void shouldSortConflictingRulesByStrictDomainPriority() {
        TelemetryRecord record = baseHeatingRecord();
        record.setDomeTemp(1300.0);      // недогретый купол (условие для R4 и R6)
        record.setGasPressure(4.2);      // SAFETY_GAS_PRESSURE (ранг 2)
        record.setWasteGasTemp(460.0);   // DHN_WASTE_OVERHEAT (ранг 3)
        record.setFlueCO(2500.0);        // R3_INCOMPLETE_COMBUSTION (ранг 5)
        record.setGasCalorific(3.7);     // R4_CALORIFIC_DROP (ранг 9)
        record.setFlueO2(3.5);           // R6_O2_EXCESS (ранг 10)

        List<Recommendation> recs = ruleEngine.evaluate(record, List.of());

        assertEquals(
            List.of(
                "SAFETY_GAS_PRESSURE",
                "DHN_WASTE_OVERHEAT",
                "R3_INCOMPLETE_COMBUSTION",
                "R4_CALORIFIC_DROP",
                "R6_O2_EXCESS"
            ),
            recs.stream().map(Recommendation::ruleCode).toList()
        );
    }

    private TelemetryRecord baseHeatingRecord() {
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setPhase(Phase.HEATING);
        record.setDomeTemp(1350.0);
        record.setWasteGasTemp(300.0);
        record.setMixedGasFlow(46000.0);
        record.setAirGasRatio(0.973);
        record.setEnrichmentNg(3.0);
        record.setGasCalorific(4.2);
        record.setGasPressure(8.1);
        record.setFlueO2(1.0);
        record.setFlueCO(40.0);
        return record;
    }
}
