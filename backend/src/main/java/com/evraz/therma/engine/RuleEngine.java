package com.evraz.therma.engine;

import com.evraz.therma.dto.*;
import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuleEngine {

    private final CombustionCalculator calc;
    private final TrendAnalyzer trendAnalyzer;

    private static final double DOME_TEMP_CRITICAL = 1390.0;
    private static final double DOME_TEMP_WARNING_MIN = 1300.0;
    private static final double DOME_TEMP_WARNING_MAX = 1349.0;
    private static final double DOME_TEMP_MAX = 1400.0;
    private static final double WASTE_GAS_WARNING = 380.0;
    private static final double WASTE_GAS_CRITICAL = 400.0;
    private static final double WASTE_GAS_BLOCK = 450.0;
    private static final double AIR_GAS_RATIO_MIN = 1.1;
    private static final double AIR_GAS_RATIO_MAX = 1.4;
    private static final double O2_MIN = 0.3;
    private static final double O2_MAX = 3.0;
    private static final double CO_MAX = 1000.0;
    private static final double Q_MIX_MIN = 3.5;
    private static final double BLAST_UNDERHEAT_DELTA = 25.0;

    public RuleEngine(CombustionCalculator calc, TrendAnalyzer trendAnalyzer) {
        this.calc = calc;
        this.trendAnalyzer = trendAnalyzer;
    }

    public List<Recommendation> evaluate(
        TelemetryRecord record,
        List<DeviationEvent> deviationEvents,
        List<DataPoint> domeHistory,
        List<DataPoint> wasteHistory) {

        List<Recommendation> recommendations = new ArrayList<>();

        if (record == null) {
            return recommendations;
        }

        // R7: проверка достаточности данных (скачки, застывания)
        Recommendation r7 = checkR7(record, domeHistory, wasteHistory);
        if (r7 != null) {
            recommendations.add(r7);
            return recommendations; // R7 блокирует все остальные
        }

        // Блокировки безопасности
        Recommendation block = checkSafetyBlocks(record);
        if (block != null) {
            recommendations.add(block);
        }

        // R3: неполное горение (CO, O2)
        Recommendation r3 = checkR3(record);
        if (r3 != null) recommendations.add(r3);

        // R1: купол растёт
        Recommendation r1 = checkR1(record, domeHistory);
        if (r1 != null) recommendations.add(r1);

        // R2: дымы растут к 400
        Recommendation r2 = checkR2(record, wasteHistory);
        if (r2 != null) recommendations.add(r2);

        // R5: прогноз T_вых < уставки
        Recommendation r5 = checkR5(record);
        if (r5 != null) recommendations.add(r5);

        // R4: Q_см упала, купол ниже цели
        Recommendation r4 = checkR4(record);
        if (r4 != null) recommendations.add(r4);

        // R6: O2 > 3% при низком куполе
        Recommendation r6 = checkR6(record);
        if (r6 != null) recommendations.add(r6);

        // Простые пороговые правила из матрицы (если не покрыты R1-R6)
        for (DeviationEvent event : deviationEvents) {
            if (!isRuleAlreadyPresent(recommendations, event.getRuleCode())) {
                Recommendation rec = processSimpleEvent(record, event);
                if (rec != null) {
                    recommendations.add(rec);
                }
            }
        }

        // Сортировка по приоритету
        recommendations.sort((a, b) -> a.getPriority().ordinal() - b.getPriority().ordinal());

        return recommendations;
    }

    public List<Recommendation> evaluate(TelemetryRecord record, List<DeviationEvent> deviationEvents) {
        return evaluate(record, deviationEvents != null ? deviationEvents : List.of(), List.of(), List.of());
    }

    // R7: недостаточность данных

    private Recommendation checkR7(TelemetryRecord record, List<DataPoint> domeHistory, List<DataPoint> wasteHistory) {
        // Скачок купола > 50 °C/мин
        if (domeHistory != null && trendAnalyzer.hasSpike(domeHistory, 50.0)) {
            return createRecommendation(RuleCode.R7_INSUFFICIENT_DATA, Priority.CRITICAL,
                "Скачок температуры купола > 50 °C/мин — данные недостоверны",
                0.0, 0.0);
        }

        // Застывшие показания (> 10 мин без изменения)
        if (domeHistory != null && trendAnalyzer.isFrozen(domeHistory, 0.5, 10)) {
            return createRecommendation(RuleCode.R7_INSUFFICIENT_DATA, Priority.WARNING,
                "Температура купола не меняется > 10 мин — возможен обрыв связи",
                0.0, 0.0);
        }

        return null;
    }

    // Блокировки безопасности

    private Recommendation checkSafetyBlocks(TelemetryRecord record) {
        // Дымы > 450 °C — немедленный перевод в резерв
        if (record.getWasteGasTemp() != null && record.getWasteGasTemp() > WASTE_GAS_BLOCK) {
            return createRecommendation(RuleCode.DHN_WASTE_OVERHEAT, Priority.CRITICAL,
                "Температура дымовых газов > 450 °C — немедленный перевод ВН в резерв",
                -100.0, -100.0);
        }

        // Купол > 1400 °C — разрушение огнеупора
        if (record.getDomeTemp() != null && record.getDomeTemp() > DOME_TEMP_MAX) {
            return createRecommendation(RuleCode.DHN_DOME_OVERHEAT, Priority.CRITICAL,
                "Температура купола > 1400 °C — риск разрушения огнеупора, снизить расход газа",
                -80.0, -10.0);
        }

        return null;
    }

    // R1: купол растёт

    private Recommendation checkR1(TelemetryRecord record, List<DataPoint> domeHistory) {
        if (record.getDomeTemp() == null || record.getDomeTemp() <= DOME_TEMP_CRITICAL) {
            return null;
        }
        if (domeHistory == null || domeHistory.size() < 2) {
            return null;
        }

        double rate = trendAnalyzer.calculateRateOfChange(domeHistory);
        if (rate > 0) {
            // α +0,05…0,15
            double currentAlpha = record.getAirGasRatio() != null && calc.calculateL0Mixed(record.getEnrichmentNg()) > 0
                ? calc.calculateAlpha(record.getAirGasRatio(), calc.calculateL0Mixed(record.getEnrichmentNg()))
                : 1.10;
            double newAlphaMin = currentAlpha + 0.05;
            double newAlphaMax = currentAlpha + 0.15;

            return createRecommendation(RuleCode.R1_DOME_GROWING, Priority.WARNING,
                String.format("Купол %.0f °C и растёт (%.1f °C/мин). Увеличить α с %.2f до %.2f–%.2f",
                    record.getDomeTemp(), rate, currentAlpha, newAlphaMin, newAlphaMax),
                -60.0, 0.0);
        }

        return null;
    }

    // R2: дымы растут к 400

    private Recommendation checkR2(TelemetryRecord record, List<DataPoint> wasteHistory) {
        if (record.getWasteGasTemp() == null || record.getWasteGasTemp() <= WASTE_GAS_WARNING) {
            return null;
        }
        if (wasteHistory == null || wasteHistory.size() < 2) {
            return null;
        }

        double rate = trendAnalyzer.calculateRateOfChange(wasteHistory);
        double forecast10 = trendAnalyzer.forecast(wasteHistory, 10);

        if (forecast10 >= WASTE_GAS_CRITICAL) {
            double gasReductionPct = 15.0;
            if (record.getWasteGasTemp() > WASTE_GAS_CRITICAL) {
                gasReductionPct = 25.0;
            }

            return createRecommendation(RuleCode.R2_WASTE_TREND, Priority.WARNING,
                String.format("Дымы %.0f °C и растут (%.1f °C/мин). Прогноз через 10 мин: %.0f °C. Снизить расход газа на %.0f%%",
                    record.getWasteGasTemp(), rate, forecast10, gasReductionPct),
                -20.0, -gasReductionPct / 100.0 * 50000.0);
        }

        return null;
    }

    // R3: неполное горение

    private Recommendation checkR3(TelemetryRecord record) {
        boolean coHigh = record.getFlueCO() != null && record.getFlueCO() > CO_MAX;
        boolean o2Low = record.getFlueO2() != null && record.getFlueO2() < O2_MIN;

        if (!coHigh && !o2Low) {
            return null;
        }

        double currentAlpha = record.getAirGasRatio() != null && calc.calculateL0Mixed(record.getEnrichmentNg()) > 0
            ? calc.calculateAlpha(record.getAirGasRatio(), calc.calculateL0Mixed(record.getEnrichmentNg()))
            : 1.05;

        return createRecommendation(RuleCode.R3_INCOMPLETE_COMBUSTION, Priority.CRITICAL,
            String.format("Неполное горение: CO=%.0f ppm, O₂=%.2f%%. Увеличить α до 1.15–1.25",
                record.getFlueCO() != null ? record.getFlueCO() : 0,
                record.getFlueO2() != null ? record.getFlueO2() : 0),
            0.0, 0.0);
    }

    // R4: Q_см упала, купол ниже цели

    private Recommendation checkR4(TelemetryRecord record) {
        if (record.getGasCalorific() == null || record.getDomeTemp() == null) {
            return null;
        }

        double qNorm = 4.2; // типовая теплота
        double qDropPct = (qNorm - record.getGasCalorific()) / qNorm * 100.0;
        double domeTarget = 1350.0;

        if (qDropPct > 5.0 && record.getDomeTemp() < domeTarget) {
            double requiredGas = calc.calculateRequiredGasFlow(
                record.getBlastFlow() != null ? record.getBlastFlow() : 4000.0,
                record.getHotBlastSetpoint() != null ? record.getHotBlastSetpoint() : 1150.0,
                record.getColdBlastTemp() != null ? record.getColdBlastTemp() : 180.0,
                record.getGasCalorific()
            );

            return createRecommendation(RuleCode.R4_CALORIFIC_DROP, Priority.WARNING,
                String.format("Q_см упала на %.1f%%, купол %.0f °C < цели %.0f °C. Требуемый расход газа: %.0f м³/ч",
                    qDropPct, record.getDomeTemp(), domeTarget, requiredGas),
                40.0, requiredGas - 46000.0);
        }

        return null;
    }

    // R5: прогноз T_вых < уставки

    private Recommendation checkR5(TelemetryRecord record) {
        if (record.getHotBlastSetpoint() == null || record.getHotBlastTemp() == null) {
            return null;
        }

        double delta = record.getHotBlastSetpoint() - record.getHotBlastTemp();
        double mixingShare = record.getMixingColdShare() != null ? record.getMixingColdShare() : 0.0;

        // Если смеситель закрыт (доля подмеса ≈ 0) и температура ниже уставки на 25 °C
        if (mixingShare < 1.0 && delta > BLAST_UNDERHEAT_DELTA) {
            double requiredGas = calc.calculateRequiredGasFlow(
                record.getBlastFlow() != null ? record.getBlastFlow() : 4000.0,
                record.getHotBlastSetpoint(),
                record.getColdBlastTemp() != null ? record.getColdBlastTemp() : 180.0,
                record.getGasCalorific() != null ? record.getGasCalorific() : 4.2
            );

            return createRecommendation(RuleCode.R5_BLAST_UNDERHEAT, Priority.WARNING,
                String.format("Прогноз: T_гд не удержит уставку %.0f °C (текущая %.0f °C, смеситель закрыт). Увеличить расход газа до %.0f м³/ч",
                    record.getHotBlastSetpoint(), record.getHotBlastTemp(), requiredGas),
                20.0, requiredGas - 46000.0);
        }

        return null;
    }

    // R6: O2 > 3% при низком куполе

    private Recommendation checkR6(TelemetryRecord record) {
        if (record.getFlueO2() == null || record.getFlueO2() <= O2_MAX) {
            return null;
        }
        if (record.getDomeTemp() == null || record.getDomeTemp() >= DOME_TEMP_MAX - 50) {
            return null;
        }

        double currentAlpha = record.getAirGasRatio() != null && calc.calculateL0Mixed(record.getEnrichmentNg()) > 0
            ? calc.calculateAlpha(record.getAirGasRatio(), calc.calculateL0Mixed(record.getEnrichmentNg()))
            : 1.20;
        double newAlphaMin = Math.max(1.05, currentAlpha - 0.10);
        double newAlphaMax = Math.max(1.05, currentAlpha - 0.05);

        return createRecommendation(RuleCode.R6_O2_EXCESS, Priority.INFO,
            String.format("O₂=%.2f%% при куполе %.0f °C. Снизить α с %.2f до %.2f–%.2f для экономии газа",
                record.getFlueO2(), record.getDomeTemp(), currentAlpha, newAlphaMin, newAlphaMax),
            30.0, -1500.0);
    }

    // Простые пороговые правила из матрицы

    private Recommendation processSimpleEvent(TelemetryRecord record, DeviationEvent event) {
        return switch (event.getRuleCode()) {
            case DHN_DOME_OVERHEAT -> createRecommendation(RuleCode.DHN_DOME_OVERHEAT, Priority.CRITICAL,
                "Критическая температура купола. Снизить расход газа на 5-10%.", -60.0, -5000.0);
            case WARN_DOME_APPROACH -> createRecommendation(RuleCode.WARN_DOME_APPROACH, Priority.WARNING,
                "Температура купола приближается к критической. Снизить расход природного газа.", -20.0, -2000.0);
            case DHN_WASTE_OVERHEAT -> createRecommendation(RuleCode.DHN_WASTE_OVERHEAT, Priority.CRITICAL,
                "Критическая температура дымовых газов. Остановка горения.", -100.0, -100000.0);
            case WARN_AIR_DEFICIT -> createRecommendation(RuleCode.WARN_AIR_DEFICIT, Priority.WARNING,
                "Недостаток воздуха для горения. Увеличить подачу воздуха.", -5.0, 0.0);
            case TWO_AIR_EXCESS -> createRecommendation(RuleCode.TWO_AIR_EXCESS, Priority.INFO,
                "Избыток воздуха. Снизить расход воздуха горения.", -2.0, 0.0);
            case INFO_BLAST_TEMP_DROP -> createRecommendation(RuleCode.INFO_BLAST_TEMP_DROP, Priority.INFO,
                "Температура горячего дутья ниже уставки. Перекидка (смена купола).", 20.0, 0.0);
            default -> null;
        };
    }

    // Вспомогательные методы

    private Recommendation createRecommendation(RuleCode ruleCode, Priority priority, String message,
                                                double tempDelta, double gasDelta) {
        Recommendation rec = new Recommendation();
        rec.setCreatedAt(Instant.now());
        rec.setRuleCode(ruleCode.name());
        rec.setPriority(priority);
        rec.setStatus(RecommendationStatus.PENDING);
        rec.setMessage(message);

        ExpectedEffect effect = new ExpectedEffect();
        effect.setTempDelta(tempDelta);
        effect.setGasDelta(gasDelta);
        rec.setExpectedEffect(effect);

        return rec;
    }

    private boolean isRuleAlreadyPresent(List<Recommendation> recommendations, RuleCode ruleCode) {
        return recommendations.stream()
            .anyMatch(r -> ruleCode.name().equals(r.getRuleCode()));
    }
}
