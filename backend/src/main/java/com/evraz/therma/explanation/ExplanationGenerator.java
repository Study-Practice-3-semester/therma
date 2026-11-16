package com.evraz.therma.explanation;

import com.evraz.therma.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExplanationGenerator {

    public List<String> generate(Recommendation recommendation, TelemetryRecord record) {
        List<String> explanations = new ArrayList<>();

        if (recommendation == null || record == null) {
            return explanations;
        }

        explanations.add("Рекомендация сформирована на основе анализа телеметрии ВН " + record.getCowperId());

        if (recommendation.getRuleCode() != null) {
            explanations.add("Правило: " + formatRuleCode(recommendation.getRuleCode()));
        }

        // Параметры, вызвавшие срабатывание
        if (record.getDomeTemp() != null) {
            explanations.add(String.format("Температура купола: %.1f °C", record.getDomeTemp()));
        }
        if (record.getWasteGasTemp() != null) {
            explanations.add(String.format("Температура дымовых газов: %.1f °C", record.getWasteGasTemp()));
        }
        if (record.getAirGasRatio() != null) {
            explanations.add(String.format("Соотношение воздух/газ: %.3f", record.getAirGasRatio()));
        }
        if (record.getHotBlastTemp() != null) {
            explanations.add(String.format("Температура горячего дутья: %.1f °C", record.getHotBlastTemp()));
        }

        // Ожидаемый эффект
        if (recommendation.getExpectedEffect() != null) {
            ExpectedEffect effect = recommendation.getExpectedEffect();
            if (effect.getTempDelta() != null && effect.getTempDelta() != 0) {
                explanations.add(String.format("Ожидаемое изменение температуры: %+.1f °C", effect.getTempDelta()));
            }
            if (effect.getGasDelta() != null && effect.getGasDelta() != 0) {
                explanations.add(String.format("Ожидаемое изменение расхода газа: %+.1f м³/ч", effect.getGasDelta()));
            }
        }

        return explanations;
    }

    private String formatRuleCode(String ruleCode) {
        switch (ruleCode) {
            case "DHN_DOME_OVERHEAT":
                return "DHN_DOME_OVERHEAT — критическая температура купола (>1390°C)";
            case "WARN_DOME_APPROACH":
                return "WARN_DOME_APPROACH — температура купола приближается к критической (1300-1349°C)";
            case "DHN_WASTE_OVERHEAT":
                return "DHN_WASTE_OVERHEAT — критическая температура дымовых газов (>350°C)";
            case "WARN_AIR_DEFICIT":
                return "WARN_AIR_DEFICIT — недостаток воздуха для горения (α<1.1)";
            case "TWO_AIR_EXCESS":
                return "TWO_AIR_EXCESS — избыток воздуха (α>1.4)";
            case "INFO_BLAST_TEMP_DROP":
                return "INFO_BLAST_TEMP_DROP — температура горячего дутья ниже уставки";
            default:
                return ruleCode;
        }
    }
}
