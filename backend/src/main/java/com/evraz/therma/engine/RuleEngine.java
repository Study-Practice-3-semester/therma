package com.evraz.therma.engine;

import com.evraz.therma.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Расчётное ядро (п. 5.3 задания).
 * Формирует рекомендуемый диапазон управляющих воздействий
 * строго по заданным экспертным правилам.
 */
@Component
public class RuleEngine {

    // Приоритет правил (чем меньше число, тем выше приоритет)
    private static final int PRIORITY_R7 = 0;
    private static final int PRIORITY_SAFETY = 1;
    private static final int PRIORITY_R3 = 2;
    private static final int PRIORITY_R1 = 3;
    private static final int PRIORITY_R2 = 4;
    private static final int PRIORITY_R5 = 5;
    private static final int PRIORITY_R4 = 6;
    private static final int PRIORITY_R6 = 7;

    /**
     * Обработка телеметрии и генерация рекомендаций.
     * @param record унифицированная запись телеметрии
     * @param deviationEvents список событий отклонений от детектора
     * @return список рекомендаций (может быть пустым)
     */
    public List<Recommendation> evaluate(TelemetryRecord record, List<DeviationEvent> deviationEvents) {
        List<Recommendation> recommendations = new ArrayList<>();

        if (record == null || deviationEvents == null) {
            return recommendations;
        }

        // Обработка каждого события отклонения
        for (DeviationEvent event : deviationEvents) {
            Recommendation recommendation = processEvent(record, event);
            if (recommendation != null) {
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    /**
     * Обработка одного события отклонения и формирование рекомендации.
     */
    private Recommendation processEvent(TelemetryRecord record, DeviationEvent event) {
        Recommendation recommendation = new Recommendation();
        recommendation.setCowperId(record.getCowperId());
        recommendation.setCreatedAt(Instant.now());
        recommendation.setRuleCode(event.getRuleCode().name());
        recommendation.setPriority(event.getPriority());
        recommendation.setStatus(RecommendationStatus.PENDING);

        // Формирование сообщения и рекомендуемого диапазона в зависимости от правила
        switch (event.getRuleCode()) {
            case DHN_DOME_OVERHEAT:
                recommendation.setMessage("Критическая температура купола. Рекомендуется снизить расход газа на 5-10%.");
                recommendation.setExpectedEffect(createExpectedEffect(-60.0, -5.0));
                break;

            case WARN_DOME_APPROACH:
                recommendation.setMessage("Температура купола приближается к критической. Рекомендуется снизить расход природного газа.");
                recommendation.setExpectedEffect(createExpectedEffect(-20.0, -2.0));
                break;

            case DHN_WASTE_OVERHEAT:
                recommendation.setMessage("Критическая температура дымовых газов. Рекомендуется остановка горения.");
                recommendation.setExpectedEffect(createExpectedEffect(-100.0, -100.0));
                break;

            case WARN_AIR_DEFICIT:
                recommendation.setMessage("Недостаток воздуха для горения. Рекомендуется увеличить подачу воздуха.");
                recommendation.setExpectedEffect(createExpectedEffect(-5.0, 0.0));
                break;

            case TWO_AIR_EXCESS:
                recommendation.setMessage("Избыток воздуха. Рекомендуется снизить расход воздуха горения.");
                recommendation.setExpectedEffect(createExpectedEffect(-2.0, 0.0));
                break;

            case INFO_BLAST_TEMP_DROP:
                recommendation.setMessage("Температура горячего дутья ниже уставки. Рекомендуется перекидка (смена купола).");
                recommendation.setExpectedEffect(createExpectedEffect(20.0, 0.0));
                break;

            default:
                recommendation.setMessage("Обнаружено отклонение. Требуется анализ.");
                recommendation.setExpectedEffect(createExpectedEffect(0.0, 0.0));
                break;
        }

        return recommendation;
    }

    /**
     * Создание объекта ExpectedEffect.
     */
    private ExpectedEffect createExpectedEffect(Double tempDelta, Double gasDelta) {
        ExpectedEffect effect = new ExpectedEffect();
        effect.setTempDelta(tempDelta);
        effect.setGasDelta(gasDelta);
        return effect;
    }
}
