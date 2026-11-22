package com.evraz.therma.engine;

import com.evraz.therma.detector.TrendAnalyzer;
import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RuleEngine {

    private final CombustionCalculator calc;
    private final TrendAnalyzer trendAnalyzer;

    // Технологические пороги и ограничения (методичка, п. 3 и п. 4)
    private static final double DOME_TEMP_CRITICAL = 1390.0;
    private static final double DOME_TEMP_TARGET = 1350.0;
    private static final double DOME_TEMP_MAX = 1400.0;

    private static final double WASTE_GAS_WARNING = 380.0;
    private static final double WASTE_GAS_CRITICAL = 400.0;
    private static final double WASTE_GAS_BLOCK = 450.0;

    private static final double GAS_PRESSURE_BLOCK = 5.0;

    private static final double ALPHA_MIN = 1.02;
    private static final double ALPHA_MAX = 1.40;
    private static final double O2_MIN = 0.3;
    private static final double O2_MAX = 3.0;
    private static final double CO_MAX = 1000.0;

    private static final double GAS_FLOW_MIN = 25_000.0;
    private static final double GAS_FLOW_MAX = 65_000.0;
    private static final double DEFAULT_GAS_FLOW = 46_000.0;
    private static final double Q_NORM = 4.2;

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

        // 1. R7: контроль достоверности данных выполняется на любой фазе и блокирует остальные правила
        Recommendation r7 = checkR7(record, domeHistory);
        if (r7 != null) {
            recommendations.add(r7);
            return recommendations;
        }

        Phase phase = record.getPhase();
        boolean checkHeating = phase == null || phase == Phase.HEATING;
        boolean checkBlowing = phase == null || phase == Phase.BLOWING;

        // 2. Правила фазы нагрева (HEATING)
        if (checkHeating) {
            addIfPresent(recommendations, checkSafetyBlocks(record));
            addIfPresent(recommendations, checkR3(record));
            addIfPresent(recommendations, checkR1(record, domeHistory));
            addIfPresent(recommendations, checkR2(record, wasteHistory));
            addIfPresent(recommendations, checkR4(record));
            addIfPresent(recommendations, checkR6(record));
        }

        // 3. Правила фазы дутья (BLOWING)
        if (checkBlowing) {
            addIfPresent(recommendations, checkR5(record));
        }

        // 4. Вторичные пороговые отклонения из детектора (если не покрыты основными правилами)
        if (deviationEvents != null) {
            for (DeviationEvent event : deviationEvents) {
                if (isEventAllowedInPhase(event.getRuleCode(), checkHeating, checkBlowing)
                    && !isRuleAlreadyPresent(recommendations, event.getRuleCode())) {
                    addIfPresent(recommendations, processSimpleEvent(record, event));
                }
            }
        }

        // 5. Разрешение конфликтов: R7 > Safety > R3 > R1 > R2 > R5 > R4 > R6 > прочие
        recommendations.sort(Comparator.comparingInt(r -> getRuleRank(r.ruleCode())));

        return recommendations;
    }

    public List<Recommendation> evaluate(TelemetryRecord record, List<DeviationEvent> deviationEvents) {
        return evaluate(record, deviationEvents != null ? deviationEvents : List.of(), List.of(), List.of());
    }

    // R7: недостаточность / недостоверность данных
    private Recommendation checkR7(TelemetryRecord record, List<DataPoint> domeHistory) {
        if (domeHistory != null && trendAnalyzer.hasSpike(domeHistory, 50.0)) {
            return createRecommendation(record, RuleCode.R7_INSUFFICIENT_DATA, Priority.CRITICAL,
                "Скачок температуры купола > 50 °C/мин — данные недостоверны",
                0.0, 0.0, null, null, null);
        }

        if (domeHistory != null && trendAnalyzer.isFrozen(domeHistory, 0.5, 10)) {
            return createRecommendation(record, RuleCode.R7_INSUFFICIENT_DATA, Priority.WARNING,
                "Температура купола не меняется > 10 мин — возможен обрыв связи",
                0.0, 0.0, null, null, null);
        }

        return null;
    }

    // Блокировки безопасности (HEATING)
    private Recommendation checkSafetyBlocks(TelemetryRecord record) {
        double currentGas = resolveCurrentGasFlow(record);

        if (record.getGasPressure() != null && record.getGasPressure() < GAS_PRESSURE_BLOCK) {
            return createRecommendation(record, RuleCode.SAFETY_GAS_PRESSURE, Priority.CRITICAL,
                String.format("Критически низкое давление газа (%.2f кПа < 5.0 кПа) — риск погасания горелки, блокировка нагрева",
                    record.getGasPressure()),
                0.0, -currentGas, "mixed_gas_flow", 0.0, 0.0);
        }

        if (record.getWasteGasTemp() != null && record.getWasteGasTemp() > WASTE_GAS_BLOCK) {
            return createRecommendation(record, RuleCode.DHN_WASTE_OVERHEAT, Priority.CRITICAL,
                "Температура дымовых газов > 450 °C — немедленный перевод ВН в резерв",
                -100.0, -currentGas, "mixed_gas_flow", 0.0, 0.0);
        }

        if (record.getDomeTemp() != null && record.getDomeTemp() > DOME_TEMP_MAX) {
            double minGas = clampGasFlow(currentGas * 0.85);
            double maxGas = clampGasFlow(currentGas * 0.90);
            return createRecommendation(record, RuleCode.DHN_DOME_OVERHEAT, Priority.CRITICAL,
                "Температура купола > 1400 °C — риск разрушения огнеупора, снизить расход газа",
                -80.0, maxGas - currentGas, "mixed_gas_flow", minGas, maxGas);
        }

        return null;
    }

    // R1: купол > 1390 °C и растёт -> увеличить α на +0.05..+0.15 без изменения расхода газа
    private Recommendation checkR1(TelemetryRecord record, List<DataPoint> domeHistory) {
        if (record.getDomeTemp() == null || record.getDomeTemp() <= DOME_TEMP_CRITICAL) {
            return null;
        }
        if (domeHistory == null || domeHistory.size() < 2) {
            return null;
        }

        double rate = trendAnalyzer.calculateRateOfChange(domeHistory);
        if (rate > 0) {
            double l0 = calc.resolveL0Mixed(record.getEnrichmentNg(), record.getGasCalorific());
            double currentAlpha = resolveCurrentAlpha(record, l0, 1.10);
            double newAlphaMin = clampAlpha(currentAlpha + 0.05);
            double newAlphaMax = clampAlpha(currentAlpha + 0.15);

            return createRecommendation(record, RuleCode.R1_DOME_GROWING, Priority.WARNING,
                String.format("Купол %.0f °C и растёт (%.1f °C/мин). Увеличить α с %.2f до %.2f–%.2f",
                    record.getDomeTemp(), rate, currentAlpha, newAlphaMin, newAlphaMax),
                -60.0, 0.0, "air_gas_ratio", newAlphaMin * l0, newAlphaMax * l0);
        }

        return null;
    }

    // R2: дымы > 380 °C и тренд к 400 °C -> снизить расход газа на 10–25%
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
            double currentGas = resolveCurrentGasFlow(record);
            boolean criticalWaste = record.getWasteGasTemp() > WASTE_GAS_CRITICAL;
            double reductionMinPct = criticalWaste ? 20.0 : 10.0;
            double reductionMaxPct = criticalWaste ? 25.0 : 15.0;

            double minGas = clampGasFlow(currentGas * (1.0 - reductionMaxPct / 100.0));
            double maxGas = clampGasFlow(currentGas * (1.0 - reductionMinPct / 100.0));
            double avgGas = (minGas + maxGas) / 2.0;

            return createRecommendation(record, RuleCode.R2_WASTE_TREND, Priority.WARNING,
                String.format("Дымы %.0f °C и растут (%.1f °C/мин). Прогноз через 10 мин: %.0f °C. Снизить расход газа на %.0f–%.0f%%",
                    record.getWasteGasTemp(), rate, forecast10, reductionMinPct, reductionMaxPct),
                -20.0, avgGas - currentGas, "mixed_gas_flow", minGas, maxGas);
        }

        return null;
    }

    // R3: неполное горение (CO > 1000 ppm или O2 < 0.3%) -> увеличить α до 1.15–1.25
    private Recommendation checkR3(TelemetryRecord record) {
        boolean coHigh = record.getFlueCO() != null && record.getFlueCO() > CO_MAX;
        boolean o2Low = record.getFlueO2() != null && record.getFlueO2() < O2_MIN;

        if (!coHigh && !o2Low) {
            return null;
        }

        double l0 = calc.resolveL0Mixed(record.getEnrichmentNg(), record.getGasCalorific());
        double targetAlphaMin = 1.15;
        double targetAlphaMax = 1.25;

        return createRecommendation(record, RuleCode.R3_INCOMPLETE_COMBUSTION, Priority.CRITICAL,
            String.format("Неполное горение: CO=%.0f ppm, O₂=%.2f%%. Увеличить α до %.2f–%.2f",
                record.getFlueCO() != null ? record.getFlueCO() : 0.0,
                record.getFlueO2() != null ? record.getFlueO2() : 0.0,
                targetAlphaMin, targetAlphaMax),
            0.0, 0.0, "air_gas_ratio", targetAlphaMin * l0, targetAlphaMax * l0);
    }

    // R4: теплота сгорания Q_см упала > 5% и купол ниже цели -> пересчитать расход газа по балансу
    private Recommendation checkR4(TelemetryRecord record) {
        if (record.getGasCalorific() == null || record.getDomeTemp() == null || record.getGasCalorific() <= 0) {
            return null;
        }

        double qDropPct = (Q_NORM - record.getGasCalorific()) / Q_NORM * 100.0;
        if (qDropPct > 5.0 && record.getDomeTemp() < DOME_TEMP_TARGET) {
            double currentGas = resolveCurrentGasFlow(record);
            double requiredGas = calc.calculateRequiredGasFlow(
                record.getBlastFlow() != null ? record.getBlastFlow() : 4000.0,
                record.getHotBlastSetpoint() != null ? record.getHotBlastSetpoint() : 1150.0,
                record.getColdBlastTemp() != null ? record.getColdBlastTemp() : 180.0,
                record.getGasCalorific()
            );

            double minGas = clampGasFlow(requiredGas * 0.97);
            double maxGas = clampGasFlow(requiredGas * 1.03);

            return createRecommendation(record, RuleCode.R4_CALORIFIC_DROP, Priority.WARNING,
                String.format("Q_см упала на %.1f%%, купол %.0f °C < цели %.0f °C. Требуемый расход газа: %.0f м³/ч",
                    qDropPct, record.getDomeTemp(), DOME_TEMP_TARGET, requiredGas),
                40.0, requiredGas - currentGas, "mixed_gas_flow", minGas, maxGas);
        }

        return null;
    }

    // R5: недогрев дутья в фазе BLOWING при закрытом смесительном клапане
    private Recommendation checkR5(TelemetryRecord record) {
        if (record.getHotBlastSetpoint() == null || record.getHotBlastTemp() == null) {
            return null;
        }

        double delta = record.getHotBlastSetpoint() - record.getHotBlastTemp();
        double mixingShare = record.getMixingColdShare() != null ? record.getMixingColdShare() : 0.0;

        if (mixingShare < 1.0 && delta > BLAST_UNDERHEAT_DELTA) {
            double currentGas = resolveCurrentGasFlow(record);
            double qMixed = record.getGasCalorific() != null && record.getGasCalorific() > 0
                ? record.getGasCalorific()
                : Q_NORM;

            double requiredGas = calc.calculateRequiredGasFlow(
                record.getBlastFlow() != null ? record.getBlastFlow() : 4000.0,
                record.getHotBlastSetpoint(),
                record.getColdBlastTemp() != null ? record.getColdBlastTemp() : 180.0,
                qMixed
            );

            double minGas = clampGasFlow(requiredGas * 0.97);
            double maxGas = clampGasFlow(requiredGas * 1.03);

            return createRecommendation(record, RuleCode.R5_BLAST_UNDERHEAT, Priority.WARNING,
                String.format("Прогноз: T_гд не удержит уставку %.0f °C (текущая %.0f °C, смеситель закрыт). Увеличить расход газа до %.0f м³/ч",
                    record.getHotBlastSetpoint(), record.getHotBlastTemp(), requiredGas),
                20.0, requiredGas - currentGas, "mixed_gas_flow", minGas, maxGas);
        }

        return null;
    }

    // R6: избыток O2 > 3% при куполе ниже цели -> снизить α на 0.05..0.10
    private Recommendation checkR6(TelemetryRecord record) {
        if (record.getFlueO2() == null || record.getFlueO2() <= O2_MAX) {
            return null;
        }
        if (record.getDomeTemp() == null || record.getDomeTemp() >= DOME_TEMP_TARGET) {
            return null;
        }

        double l0 = calc.resolveL0Mixed(record.getEnrichmentNg(), record.getGasCalorific());
        double currentAlpha = resolveCurrentAlpha(record, l0, 1.25);
        double newAlphaMin = Math.max(1.05, currentAlpha - 0.10);
        double newAlphaMax = Math.max(1.05, currentAlpha - 0.05);

        return createRecommendation(record, RuleCode.R6_O2_EXCESS, Priority.INFO,
            String.format("O₂=%.2f%% при куполе %.0f °C. Снизить α с %.2f до %.2f–%.2f для экономии газа",
                record.getFlueO2(), record.getDomeTemp(), currentAlpha, newAlphaMin, newAlphaMax),
            30.0, -1500.0, "air_gas_ratio", newAlphaMin * l0, newAlphaMax * l0);
    }

    // Вторичные пороговые правила из матрицы
    private Recommendation processSimpleEvent(TelemetryRecord record, DeviationEvent event) {
        double currentGas = resolveCurrentGasFlow(record);
        double l0 = calc.resolveL0Mixed(record.getEnrichmentNg(), record.getGasCalorific());

        return switch (event.getRuleCode()) {
            case SAFETY_GAS_PRESSURE -> createRecommendation(record, RuleCode.SAFETY_GAS_PRESSURE, Priority.CRITICAL,
                "Критически низкое давление газа (< 5 кПа). Блокировка подачи газа.",
                0.0, -currentGas, "mixed_gas_flow", 0.0, 0.0);
            case DHN_DOME_OVERHEAT -> createRecommendation(record, RuleCode.DHN_DOME_OVERHEAT, Priority.CRITICAL,
                "Критическая температура купола. Снизить расход газа на 5–10%.",
                -60.0, -currentGas * 0.08, "mixed_gas_flow",
                clampGasFlow(currentGas * 0.90), clampGasFlow(currentGas * 0.95));
            case WARN_DOME_APPROACH -> createRecommendation(record, RuleCode.WARN_DOME_APPROACH, Priority.WARNING,
                "Температура купола приближается к пределу. Снизить расход газа на 3–5%.",
                -20.0, -currentGas * 0.04, "mixed_gas_flow",
                clampGasFlow(currentGas * 0.95), clampGasFlow(currentGas * 0.97));
            case DHN_WASTE_OVERHEAT -> createRecommendation(record, RuleCode.DHN_WASTE_OVERHEAT, Priority.CRITICAL,
                "Критическая температура дымовых газов. Остановка горения.",
                -100.0, -currentGas, "mixed_gas_flow", 0.0, 0.0);
            case WARN_AIR_DEFICIT -> createRecommendation(record, RuleCode.WARN_AIR_DEFICIT, Priority.WARNING,
                "Недостаток воздуха для горения. Увеличить коэффициент избытка воздуха α до 1.08–1.15.",
                -5.0, 0.0, "air_gas_ratio", 1.08 * l0, 1.15 * l0);
            case TWO_AIR_EXCESS -> createRecommendation(record, RuleCode.TWO_AIR_EXCESS, Priority.INFO,
                "Избыток воздуха. Снизить коэффициент избытка воздуха α до 1.08–1.15.",
                -2.0, 0.0, "air_gas_ratio", 1.08 * l0, 1.15 * l0);
            case INFO_BLAST_TEMP_DROP -> createRecommendation(record, RuleCode.INFO_BLAST_TEMP_DROP, Priority.INFO,
                "Температура горячего дутья ниже уставки. Подготовить перекидку (смену каупера).",
                20.0, 0.0, null, null, null);
            default -> null;
        };
    }

    // Приоритет разрешения конфликтов (методичка, п. 6):
    // R7 > Safety (давление, дымы > 450, купол > 1400) > R3 > R1 > R2 > R5 > R4 > R6 > вторичные
    private int getRuleRank(String ruleCode) {
        if (ruleCode == null) {
            return 100;
        }
        return switch (ruleCode) {
            case "R7_INSUFFICIENT_DATA" -> 1;
            case "SAFETY_GAS_PRESSURE" -> 2;
            case "DHN_WASTE_OVERHEAT" -> 3;
            case "DHN_DOME_OVERHEAT" -> 4;
            case "R3_INCOMPLETE_COMBUSTION" -> 5;
            case "R1_DOME_GROWING" -> 6;
            case "R2_WASTE_TREND" -> 7;
            case "R5_BLAST_UNDERHEAT" -> 8;
            case "R4_CALORIFIC_DROP" -> 9;
            case "R6_O2_EXCESS" -> 10;
            case "WARN_DOME_APPROACH" -> 11;
            case "WARN_AIR_DEFICIT" -> 12;
            case "TWO_AIR_EXCESS" -> 13;
            case "INFO_BLAST_TEMP_DROP" -> 14;
            default -> 99;
        };
    }

    private boolean isEventAllowedInPhase(RuleCode code, boolean checkHeating, boolean checkBlowing) {
        if (code == null) {
            return false;
        }
        return switch (code) {
            case INFO_BLAST_TEMP_DROP, R5_BLAST_UNDERHEAT -> checkBlowing;
            case R7_INSUFFICIENT_DATA -> true;
            default -> checkHeating;
        };
    }

    private Recommendation createRecommendation(
        TelemetryRecord record,
        RuleCode ruleCode,
        Priority priority,
        String message,
        double tempDelta,
        double gasDelta,
        String targetParameter,
        Double suggestedValueMin,
        Double suggestedValueMax) {

        Recommendation rec = new Recommendation(
            null,
            record.getCowperId(),
            Instant.now(),
            ruleCode.name(),
            message,
            priority,
            RecommendationStatus.PENDING,
            new ExpectedEffect(tempDelta, gasDelta),
            List.of(),
            targetParameter,
            suggestedValueMin,
            suggestedValueMax
        );

        return rec;
    }

    private void addIfPresent(List<Recommendation> list, Recommendation rec) {
        if (rec != null) {
            list.add(rec);
        }
    }

    private boolean isRuleAlreadyPresent(List<Recommendation> recommendations, RuleCode ruleCode) {
        return ruleCode != null && recommendations.stream()
            .anyMatch(r -> ruleCode.name().equals(r.ruleCode()));
    }

    private double resolveCurrentGasFlow(TelemetryRecord record) {
        return record.getMixedGasFlow() != null && record.getMixedGasFlow() > 0
            ? record.getMixedGasFlow()
            : DEFAULT_GAS_FLOW;
    }

    private double resolveCurrentAlpha(TelemetryRecord record, double l0, double fallbackAlpha) {
        if (record.getAirGasRatio() != null && l0 > 0) {
            return calc.calculateAlpha(record.getAirGasRatio(), l0);
        }
        return fallbackAlpha;
    }

    private double clampGasFlow(double gasFlow) {
        return Math.clamp(gasFlow, GAS_FLOW_MIN, GAS_FLOW_MAX);
    }

    private double clampAlpha(double alpha) {
        return Math.clamp(alpha, ALPHA_MIN, ALPHA_MAX);
    }
}
