package com.evraz.therma.detector;

import com.evraz.therma.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryDeviationDetectorTest {

    private TelemetryDeviationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TelemetryDeviationDetector();
    }

    @Test
    void shouldDetectCriticalDomeOverheat() {
        // Дано: температура купола выше критической (1390°C)
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1400.0);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
        assertEquals(RuleCode.DHN_DOME_OVERHEAT, events.get(0).getRuleCode());
        assertEquals(Priority.CRITICAL, events.get(0).getPriority());
        assertEquals(1400.0, events.get(0).getActualValue());
    }

    @Test
    void shouldDetectWarningDomeApproach() {
        // Дано: температура купола в диапазоне предупреждения (1300-1349°C)
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1320.0);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertFalse(events.isEmpty());
        assertEquals(RuleCode.WARN_DOME_APPROACH, events.get(0).getRuleCode());
        assertEquals(Priority.WARNING, events.get(0).getPriority());
    }

    @Test
    void shouldDetectWasteGasOverheat() {
        // Дано: температура дымовых газов выше критической (350°C)
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setWasteGasTemp(360.0);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertFalse(events.isEmpty());
        assertEquals(RuleCode.DHN_WASTE_OVERHEAT, events.get(0).getRuleCode());
        assertEquals(Priority.CRITICAL, events.get(0).getPriority());
    }

    @Test
    void shouldDetectAirDeficit() {
        // Дано: соотношение воздух/газ ниже минимума (1.1)
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setAirGasRatio(1.05);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertFalse(events.isEmpty());
        assertEquals(RuleCode.WARN_AIR_DEFICIT, events.get(0).getRuleCode());
        assertEquals(Priority.WARNING, events.get(0).getPriority());
    }

    @Test
    void shouldDetectAirExcess() {
        // Дано: соотношение воздух/газ выше максимума (1.4)
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setAirGasRatio(1.45);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertFalse(events.isEmpty());
        assertEquals(RuleCode.TWO_AIR_EXCESS, events.get(0).getRuleCode());
        assertEquals(Priority.INFO, events.get(0).getPriority());
    }

    @Test
    void shouldReturnEmptyForNormalTelemetry() {
        // Дано: все параметры в норме
        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId("VN-1");
        record.setRecordedAt(Instant.now());
        record.setDomeTemp(1350.0);
        record.setWasteGasTemp(300.0);
        record.setAirGasRatio(1.15);

        // Когда
        List<DeviationEvent> events = detector.detect(record);

        // Тогда
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullRecord() {
        // Когда
        List<DeviationEvent> events = detector.detect(null);

        // Тогда
        assertTrue(events.isEmpty());
    }
}
