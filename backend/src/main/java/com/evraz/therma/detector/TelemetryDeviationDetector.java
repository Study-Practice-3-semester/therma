package com.evraz.therma.detector;

import com.evraz.therma.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelemetryDeviationDetector {

    // Пороговые значения из матрицы экспертных правил
    private static final double DOME_TEMP_CRITICAL = 1390.0;
    private static final double DOME_TEMP_WARNING_MIN = 1300.0;
    private static final double DOME_TEMP_WARNING_MAX = 1349.0;
    private static final double WASTE_GAS_TEMP_CRITICAL = 350.0;

    // Коэффициент избытка воздуха
    private static final double AIR_GAS_RATIO_MIN = 1.1;
    private static final double AIR_GAS_RATIO_MAX = 1.4;

    public List<DeviationEvent> detect(TelemetryRecord record) {
        List<DeviationEvent> events = new ArrayList<>();

        if (record == null) {
            return events;
        }

        // Проверка температуры купола
        if (record.getDomeTemp() != null) {
            if (record.getDomeTemp() >= DOME_TEMP_CRITICAL) {
                DeviationEvent event = new DeviationEvent();
                event.setCowperId(record.getCowperId());
                event.setTimestamp(record.getRecordedAt());
                event.setRuleCode(RuleCode.DHN_DOME_OVERHEAT);
                event.setParameterName("domeTemp");
                event.setActualValue(record.getDomeTemp());
                event.setTargetMax(DOME_TEMP_CRITICAL);
                event.setPriority(Priority.CRITICAL);
                events.add(event);
            } else if (record.getDomeTemp() >= DOME_TEMP_WARNING_MIN && record.getDomeTemp() <= DOME_TEMP_WARNING_MAX) {
                DeviationEvent event = new DeviationEvent();
                event.setCowperId(record.getCowperId());
                event.setTimestamp(record.getRecordedAt());
                event.setRuleCode(RuleCode.WARN_DOME_APPROACH);
                event.setParameterName("domeTemp");
                event.setActualValue(record.getDomeTemp());
                event.setTargetMin(DOME_TEMP_WARNING_MIN);
                event.setTargetMax(DOME_TEMP_WARNING_MAX);
                event.setPriority(Priority.WARNING);
                events.add(event);
            }
        }

        // Проверка температуры дымовых газов
        if (record.getWasteGasTemp() != null && record.getWasteGasTemp() >= WASTE_GAS_TEMP_CRITICAL) {
            DeviationEvent event = new DeviationEvent();
            event.setCowperId(record.getCowperId());
            event.setTimestamp(record.getRecordedAt());
            event.setRuleCode(RuleCode.DHN_WASTE_OVERHEAT);
            event.setParameterName("wasteGasTemp");
            event.setActualValue(record.getWasteGasTemp());
            event.setTargetMax(WASTE_GAS_TEMP_CRITICAL);
            event.setPriority(Priority.CRITICAL);
            events.add(event);
        }

        // Проверка соотношения воздух/газ
        if (record.getAirGasRatio() != null) {
            if (record.getAirGasRatio() < AIR_GAS_RATIO_MIN) {
                DeviationEvent event = new DeviationEvent();
                event.setCowperId(record.getCowperId());
                event.setTimestamp(record.getRecordedAt());
                event.setRuleCode(RuleCode.WARN_AIR_DEFICIT);
                event.setParameterName("airGasRatio");
                event.setActualValue(record.getAirGasRatio());
                event.setTargetMin(AIR_GAS_RATIO_MIN);
                event.setPriority(Priority.WARNING);
                events.add(event);
            } else if (record.getAirGasRatio() > AIR_GAS_RATIO_MAX) {
                DeviationEvent event = new DeviationEvent();
                event.setCowperId(record.getCowperId());
                event.setTimestamp(record.getRecordedAt());
                event.setRuleCode(RuleCode.TWO_AIR_EXCESS);
                event.setParameterName("airGasRatio");
                event.setActualValue(record.getAirGasRatio());
                event.setTargetMax(AIR_GAS_RATIO_MAX);
                event.setPriority(Priority.INFO);
                events.add(event);
            }
        }

        return events;
    }
}
