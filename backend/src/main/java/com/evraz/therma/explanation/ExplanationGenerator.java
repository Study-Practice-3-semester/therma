package com.evraz.therma.explanation;

import com.evraz.therma.dto.*;
import com.evraz.therma.engine.CombustionCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExplanationGenerator {

    private final CombustionCalculator calc;

    @Autowired
    public ExplanationGenerator(CombustionCalculator calc) {
        this.calc = calc;
    }

    public ExplanationGenerator() {
        this(new CombustionCalculator());
    }

    public List<String> generate(Recommendation recommendation, TelemetryRecord record) {
        List<String> explanations = new ArrayList<>();
        if (recommendation == null || record == null) {
            return explanations;
        }

        String ruleCode = recommendation.getRuleCode();
        explanations.add(String.format("Аппарат: %s | Правило: %s",
            record.getCowperId() != null ? record.getCowperId() : "не указан",
            formatRuleCode(ruleCode)));

        // 1. Адресные параметры и пороги, вызвавшие срабатывание
        appendTriggerFactors(explanations, ruleCode, record);

        // 2. Рассчитанный диапазон управляющего воздействия
        appendSuggestedRange(explanations, recommendation);

        // 3. Прогнозируемый технологический эффект
        appendExpectedEffect(explanations, recommendation.getExpectedEffect());

        return explanations;
    }

    private void appendTriggerFactors(List<String> factors, String ruleCode, TelemetryRecord r) {
        if (ruleCode == null) {
            return;
        }

        switch (ruleCode) {
            case "SAFETY_GAS_PRESSURE" -> {
                if (r.getGasPressure() != null) {
                    factors.add(String.format("gasPressure < 5.0 кПа (текущее: %.2f кПа)", r.getGasPressure()));
                }
            }
            case "DHN_DOME_OVERHEAT" -> {
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp > 1400.0 °C (текущее: %.1f °C)", r.getDomeTemp()));
                }
            }
            case "WARN_DOME_APPROACH" -> {
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp > 1370.0 °C (текущее: %.1f °C, цель: 1330–1370 °C)", r.getDomeTemp()));
                }
            }
            case "R1_DOME_GROWING" -> {
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp > 1390.0 °C при положительном тренде (текущее: %.1f °C)", r.getDomeTemp()));
                }
                appendAlphaInfo(factors, r);
            }
            case "DHN_WASTE_OVERHEAT" -> {
                if (r.getWasteGasTemp() != null) {
                    double limit = r.getWasteGasTemp() > 450.0 ? 450.0 : 400.0;
                    factors.add(String.format("wasteGasTemp > %.1f °C (текущее: %.1f °C)", limit, r.getWasteGasTemp()));
                }
            }
            case "R2_WASTE_TREND" -> {
                if (r.getWasteGasTemp() != null) {
                    factors.add(String.format("wasteGasTemp > 380.0 °C с прогнозом выхода за 400 °C (текущее: %.1f °C)", r.getWasteGasTemp()));
                }
                if (r.getMixedGasFlow() != null) {
                    factors.add(String.format("Текущий расход смешанного газа: %.0f м³/ч", r.getMixedGasFlow()));
                }
            }
            case "R3_INCOMPLETE_COMBUSTION" -> {
                if (r.getFlueCO() != null && r.getFlueCO() > 1000.0) {
                    factors.add(String.format("flueCO > 1000 ppm (текущее: %.0f ppm)", r.getFlueCO()));
                }
                if (r.getFlueO2() != null && r.getFlueO2() < 0.3) {
                    factors.add(String.format("flueO2 < 0.3%% (текущее: %.2f%%)", r.getFlueO2()));
                }
                appendAlphaInfo(factors, r);
            }
            case "R4_CALORIFIC_DROP" -> {
                if (r.getGasCalorific() != null) {
                    double dropPct = (4.2 - r.getGasCalorific()) / 4.2 * 100.0;
                    factors.add(String.format("gasCalorific упала на %.1f%% от нормы 4.20 МДж/м³ (текущее: %.2f МДж/м³)",
                        dropPct, r.getGasCalorific()));
                }
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp < 1350.0 °C (текущее: %.1f °C)", r.getDomeTemp()));
                }
            }
            case "R5_BLAST_UNDERHEAT" -> {
                if (r.getHotBlastTemp() != null && r.getHotBlastSetpoint() != null) {
                    factors.add(String.format("hotBlastTemp ниже уставки более чем на 25 °C (текущее: %.1f °C, уставка: %.1f °C)",
                        r.getHotBlastTemp(), r.getHotBlastSetpoint()));
                }
                if (r.getMixingColdShare() != null) {
                    factors.add(String.format("Смесительный клапан закрыт: mixingColdShare = %.1f%% (< 1.0%%)", r.getMixingColdShare()));
                }
            }
            case "INFO_BLAST_TEMP_DROP" -> {
                if (r.getHotBlastTemp() != null && r.getHotBlastSetpoint() != null) {
                    factors.add(String.format("hotBlastTemp ниже уставки (текущее: %.1f °C, уставка: %.1f °C)",
                        r.getHotBlastTemp(), r.getHotBlastSetpoint()));
                }
            }
            case "R6_O2_EXCESS" -> {
                if (r.getFlueO2() != null) {
                    factors.add(String.format("flueO2 > 3.0%% (текущее: %.2f%%)", r.getFlueO2()));
                }
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp < 1350.0 °C (текущее: %.1f °C)", r.getDomeTemp()));
                }
                appendAlphaInfo(factors, r);
            }
            case "WARN_AIR_DEFICIT" -> {
                appendAlphaThreshold(factors, r, "α < 1.02 (дефицит воздуха)");
            }
            case "TWO_AIR_EXCESS" -> {
                appendAlphaThreshold(factors, r, "α > 1.30 (избыток воздуха)");
            }
            case "R7_INSUFFICIENT_DATA" -> {
                factors.add("Нарушена свежесть, непрерывность или физическая правдоподобность телеметрии");
                if (r.getRecordedAt() != null) {
                    factors.add("Метка времени последнего пакета: " + r.getRecordedAt());
                }
            }
            default -> {
                if (r.getDomeTemp() != null) {
                    factors.add(String.format("domeTemp = %.1f °C", r.getDomeTemp()));
                }
            }
        }
    }

    private void appendAlphaInfo(List<String> factors, TelemetryRecord r) {
        if (r.getAirGasRatio() != null && r.getAirGasRatio() > 0) {
            double l0 = calc.resolveL0Mixed(r.getEnrichmentNg(), r.getGasCalorific());
            double alpha = calc.calculateAlpha(r.getAirGasRatio(), l0);
            factors.add(String.format("Текущее соотношение воздух/газ: %.3f (α ≈ %.2f)", r.getAirGasRatio(), alpha));
        }
    }

    private void appendAlphaThreshold(List<String> factors, TelemetryRecord r, String condition) {
        if (r.getAirGasRatio() != null && r.getAirGasRatio() > 0) {
            double l0 = calc.resolveL0Mixed(r.getEnrichmentNg(), r.getGasCalorific());
            double alpha = calc.calculateAlpha(r.getAirGasRatio(), l0);
            factors.add(String.format("%s — текущее α = %.2f (air_gas_ratio = %.3f)", condition, alpha, r.getAirGasRatio()));
        }
    }

    private void appendSuggestedRange(List<String> factors, Recommendation rec) {
        String param = rec.getTargetParameter();
        Double min = rec.getSuggestedValueMin();
        Double max = rec.getSuggestedValueMax();

        if (param == null || min == null || max == null) {
            return;
        }

        if ("mixed_gas_flow".equals(param)) {
            if (min == 0.0 && max == 0.0) {
                factors.add("Управляющее воздействие: полная отсечка смешанного газа (0 м³/ч)");
            } else {
                factors.add(String.format("Рекомендуемый диапазон mixed_gas_flow: %.0f – %.0f м³/ч", min, max));
            }
        } else if ("air_gas_ratio".equals(param)) {
            factors.add(String.format("Рекомендуемый диапазон air_gas_ratio: %.3f – %.3f м³/м³", min, max));
        } else {
            factors.add(String.format("Рекомендуемый диапазон %s: %.2f – %.2f", param, min, max));
        }
    }

    private void appendExpectedEffect(List<String> factors, ExpectedEffect effect) {
        if (effect == null) {
            return;
        }
        if (effect.getTempDelta() != null && effect.getTempDelta() != 0.0) {
            factors.add(String.format("Ожидаемое изменение температуры: %+.1f °C", effect.getTempDelta()));
        }
        if (effect.getGasDelta() != null && effect.getGasDelta() != 0.0) {
            factors.add(String.format("Ожидаемое изменение расхода газа: %+.0f м³/ч", effect.getGasDelta()));
        }
    }

    private String formatRuleCode(String ruleCode) {
        if (ruleCode == null) {
            return "UNKNOWN";
        }
        return switch (ruleCode) {
            case "SAFETY_GAS_PRESSURE" -> "SAFETY_GAS_PRESSURE — аварийное падение давления газа (< 5.0 кПа)";
            case "DHN_DOME_OVERHEAT" -> "DHN_DOME_OVERHEAT — критический перегрев купола (> 1400 °C)";
            case "WARN_DOME_APPROACH" -> "WARN_DOME_APPROACH — приближение температуры купола к пределу (> 1370 °C)";
            case "DHN_WASTE_OVERHEAT" -> "DHN_WASTE_OVERHEAT — критический перегрев отходящих дымов (> 400 °C)";
            case "WARN_AIR_DEFICIT" -> "WARN_AIR_DEFICIT — дефицит воздуха для горения (α < 1.02)";
            case "TWO_AIR_EXCESS" -> "TWO_AIR_EXCESS — избыток воздуха горения (α > 1.30)";
            case "INFO_BLAST_TEMP_DROP" -> "INFO_BLAST_TEMP_DROP — снижение температуры горячего дутья ниже уставки";
            case "R1_DOME_GROWING" -> "R1_DOME_GROWING — рост температуры купола выше 1390 °C";
            case "R2_WASTE_TREND" -> "R2_WASTE_TREND — опасный тренд роста температуры дымовых газов к 400 °C";
            case "R3_INCOMPLETE_COMBUSTION" -> "R3_INCOMPLETE_COMBUSTION — химический недожог (CO > 1000 ppm или O₂ < 0.3%)";
            case "R4_CALORIFIC_DROP" -> "R4_CALORIFIC_DROP — падение теплоты сгорания газа при недогретом куполе";
            case "R5_BLAST_UNDERHEAT" -> "R5_BLAST_UNDERHEAT — исчерпание тепловой ёмкости насадки на дутье";
            case "R6_O2_EXCESS" -> "R6_O2_EXCESS — избыток кислорода в дымах (> 3.0%) при недогретом куполе";
            case "R7_INSUFFICIENT_DATA" -> "R7_INSUFFICIENT_DATA — режим «Недостаточно данных»";
            default -> ruleCode;
        };
    }
}
