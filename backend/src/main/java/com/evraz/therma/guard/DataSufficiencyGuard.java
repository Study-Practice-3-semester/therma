package com.evraz.therma.guard;

import com.evraz.therma.dto.SystemStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataSufficiencyGuard {

    @Value("${telemetry.stale-threshold-minutes:3}")
    private int staleThresholdMinutes;

    @Value("${telemetry.frozen-threshold-minutes:10}")
    private int frozenThresholdMinutes;

    private Instant lastTelemetryTimestamp;
    private Double lastDomeTemp;

    public SystemStatus check(Instant currentTimestamp, Double currentDomeTemp) {
        // Проверка обрыва связи
        if (currentTimestamp == null) {
            if (lastTelemetryTimestamp != null) {
                long minutesSinceLast = ChronoUnit.MINUTES.between(lastTelemetryTimestamp, Instant.now());
                if (minutesSinceLast > staleThresholdMinutes) {
                    return SystemStatus.INSUFFICIENT_DATA;
                }
            }
            return SystemStatus.ACTIVE;
        }

        // Проверка застывших показаний (температура не меняется)
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

    public void reset() {
        lastTelemetryTimestamp = null;
        lastDomeTemp = null;
    }
}
