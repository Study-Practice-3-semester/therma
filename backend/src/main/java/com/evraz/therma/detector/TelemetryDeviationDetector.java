package com.evraz.therma.detector;

import com.evraz.therma.dto.*;
import com.evraz.therma.engine.CombustionCalculator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelemetryDeviationDetector {

    // Пороговые значения
    private static final double DOME_TEMP_TARGET_MIN = 1330.0;
    private static final double DOME_TEMP_TARGET_MAX = 1370.0;
    private static final double DOME_TEMP_CRITICAL = 1400.0;

    private static final double WASTE_GAS_TEMP_WARNING = 380.0;
    private static final double WASTE_GAS_TEMP_CRITICAL = 400.0;

    // Коэффициент избытка воздуха α (цель 1.05–1.15, допустимо 1.02–1.30)
    private static final double ALPHA_TARGET_MIN = 1.05;
    private static final double ALPHA_TARGET_MAX = 1.15;
    private static final double ALPHA_CRITICAL_MIN = 1.02;
    private static final double ALPHA_ALLOWABLE_MAX = 1.30;

    private static final double GAS_PRESSURE_CRITICAL_MIN = 5.0;
    private static final double BLAST_UNDERHEAT_DELTA = 25.0;

    private final CombustionCalculator calc;

    public TelemetryDeviationDetector(CombustionCalculator calc) {
        this.calc = calc;
    }

    public List<DeviationEvent> detect(TelemetryRecord record) {
        List<DeviationEvent> events = new ArrayList<>();
        if (record == null) {
            return events;
        }

        Phase phase = record.getPhase();
        boolean checkHeating = phase == null || phase == Phase.HEATING;
        boolean checkBlowing = phase == null || phase == Phase.BLOWING;

        if (checkHeating) {
            detectHeatingDeviations(record, events);
        }
        if (checkBlowing) {
            detectBlowingDeviations(record, events);
        }

        return events;
    }

    private void detectHeatingDeviations(TelemetryRecord record, List<DeviationEvent> events) {
        // Аварийное падение давления газа перед горелкой (< 5 кПа)
        if (record.getGasPressure() != null && record.getGasPressure() < GAS_PRESSURE_CRITICAL_MIN) {
            events.add(buildEvent(record, RuleCode.SAFETY_GAS_PRESSURE, "gasPressure",
                record.getGasPressure(), GAS_PRESSURE_CRITICAL_MIN, 9.0, Priority.CRITICAL));
        }

        // Температура купола (цель 1330–1370 C, допуск до 1400 C, авария > 1400 C)
        if (record.getDomeTemp() != null) {
            if (record.getDomeTemp() > DOME_TEMP_CRITICAL) {
                events.add(buildEvent(record, RuleCode.DHN_DOME_OVERHEAT, "domeTemp",
                    record.getDomeTemp(), DOME_TEMP_TARGET_MIN, DOME_TEMP_CRITICAL, Priority.CRITICAL));
            } else if (record.getDomeTemp() > DOME_TEMP_TARGET_MAX) {
                events.add(buildEvent(record, RuleCode.WARN_DOME_APPROACH, "domeTemp",
                    record.getDomeTemp(), DOME_TEMP_TARGET_MIN, DOME_TEMP_TARGET_MAX, Priority.WARNING));
            }
        }

        // Температура отходящих дымов (предупреждение > 380 C, авария > 400 C)
        if (record.getWasteGasTemp() != null) {
            if (record.getWasteGasTemp() > WASTE_GAS_TEMP_CRITICAL) {
                events.add(buildEvent(record, RuleCode.DHN_WASTE_OVERHEAT, "wasteGasTemp",
                    record.getWasteGasTemp(), null, WASTE_GAS_TEMP_CRITICAL, Priority.CRITICAL));
            } else if (record.getWasteGasTemp() > WASTE_GAS_TEMP_WARNING) {
                events.add(buildEvent(record, RuleCode.R2_WASTE_TREND, "wasteGasTemp",
                    record.getWasteGasTemp(), null, WASTE_GAS_TEMP_WARNING, Priority.WARNING));
            }
        }

        // Коэффициент избытка воздуха α = air_gas_ratio / L0
        if (record.getAirGasRatio() != null && record.getAirGasRatio() > 0) {
            double l0 = calc.resolveL0Mixed(record.getEnrichmentNg(), record.getGasCalorific());
            double alpha = calc.calculateAlpha(record.getAirGasRatio(), l0);

            if (alpha < ALPHA_CRITICAL_MIN) {
                events.add(buildEvent(record, RuleCode.WARN_AIR_DEFICIT, "alpha",
                    alpha, ALPHA_TARGET_MIN, ALPHA_TARGET_MAX, Priority.WARNING));
            } else if (alpha > ALPHA_ALLOWABLE_MAX) {
                events.add(buildEvent(record, RuleCode.TWO_AIR_EXCESS, "alpha",
                    alpha, ALPHA_TARGET_MIN, ALPHA_TARGET_MAX, Priority.INFO));
            }
        }
    }

    private void detectBlowingDeviations(TelemetryRecord record, List<DeviationEvent> events) {
        if (record.getHotBlastTemp() != null && record.getHotBlastSetpoint() != null) {
            double underheat = record.getHotBlastSetpoint() - record.getHotBlastTemp();
            if (underheat > BLAST_UNDERHEAT_DELTA) {
                events.add(buildEvent(record, RuleCode.INFO_BLAST_TEMP_DROP, "hotBlastTemp",
                    record.getHotBlastTemp(), record.getHotBlastSetpoint() - 10.0,
                    record.getHotBlastSetpoint() + 10.0, Priority.INFO));
            }
        }
    }

    private DeviationEvent buildEvent(TelemetryRecord record, RuleCode ruleCode, String paramName,
                                      Double actual, Double targetMin, Double targetMax, Priority priority) {
        DeviationEvent event = new DeviationEvent();
        event.setCowperId(record.getCowperId());
        event.setTimestamp(record.getRecordedAt());
        event.setRuleCode(ruleCode);
        event.setParameterName(paramName);
        event.setActualValue(actual);
        event.setTargetMin(targetMin);
        event.setTargetMax(targetMax);
        event.setPriority(priority);
        return event;
    }
}
