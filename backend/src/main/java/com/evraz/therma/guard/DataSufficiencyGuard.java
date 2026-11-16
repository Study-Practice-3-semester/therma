package com.evraz.therma.guard;

import com.evraz.therma.dto.SystemStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Модуль обработки нештатных ситуаций (п. 5.5 задания).
 * Отслеживает свежесть телеметрии и переводит систему в режим
 * "недостаточно данных" при обрывах связи или застывших показаниях.
 */
@Component
public class DataSufficiencyGuard {

    @Value("${telemetry.stale-threshold-minutes:3}")
    private int staleThresholdMinutes;

    @Value("${telemetry.frozen-threshold-minutes:10}")
    private int frozenThresholdMinutes;

    private Instant lastTelemetryTimestamp;
    private Double lastDomeTemp;

    /**
     * Проверка достаточности данных.
     * @param currentTimestamp время последней полученной телеметрии
     * @param currentDomeTemp текущая температура купола
     * @return статус системы (ACTIVE или INSUFFICIENT_DATA)
     */
    public SystemStatus check(Instant currentTimestamp, Double currentDomeTemp) {
        // Проверка обрыва связи (нет данных > staleThreshold)
        if (currentTimestamp == null) {
            if (lastTelemetryTimestamp != null) {
                long minutesSinceLast = ChronoUnit.MINUTES.between(lastTelemetryTimestamp, Instant.now());
                if (minutesSinceLast > staleThresholdMinutes) {
                    return SystemStatus.INSUFFICIENT_DATA;
                }
            }
            return SystemStatus.ACTIVE;
        }

        // Проверка застывших показаний (температура не меняется > frozenThreshold)
        if (currentDomeTemp != null && lastDomeTemp != null) {
            if (currentDomeTemp.equals(lastDomeTemp)) {
                if (lastTelemetryTimestamp != null) {
                    long minutesFrozen = ChronoUnit.MINUTES.between(lastTelemetryTimestamp, currentTimestamp);
                    if (minutesFrozen > frozenThresholdMinutes) {
                        return SystemStatus.INSUFFICIENT_DATA;
                    }
                }
            }
        }

        // Обновляем последние значения
        lastTelemetryTimestamp = currentTimestamp;
        lastDomeTemp = currentDomeTemp;

        return SystemStatus.ACTIVE;
    }

    /**
     * Сброс состояния (при перезапуске симулятора).
     */
    public void reset() {
        lastTelemetryTimestamp = null;
        lastDomeTemp = null;
    }
}
