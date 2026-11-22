package com.evraz.therma.guard;

import com.evraz.therma.dto.SystemStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DataSufficiencyGuard {

    private record StoveState(
        Instant lastTelemetryTimestamp,
        Double lastDomeTemp,
        Instant frozenSinceTimestamp
    ) {}

    private final Clock clock;
    private final long staleThresholdMillis;
    private final long frozenThresholdMillis;
    private final double frozenTolerance;

    private final Map<String, StoveState> states = new ConcurrentHashMap<>();

    @Autowired
    public DataSufficiencyGuard(
        @Value("${telemetry.stale-threshold-minutes:3}") int staleThresholdMinutes,
        @Value("${telemetry.frozen-threshold-minutes:10}") int frozenThresholdMinutes,
        @Value("${telemetry.frozen-tolerance:0.05}") double frozenTolerance
    ) {
        this(Clock.systemUTC(), staleThresholdMinutes, frozenThresholdMinutes, frozenTolerance);
    }


    // for unit-tests
    public DataSufficiencyGuard(
        Clock clock,
        int staleThresholdMinutes,
        int frozenThresholdMinutes,
        double frozenTolerance
    ) {
        this.clock = clock;
        this.staleThresholdMillis = Duration.ofMinutes(staleThresholdMinutes).toMillis();
        this.frozenThresholdMillis = Duration.ofMinutes(frozenThresholdMinutes).toMillis();
        this.frozenTolerance = frozenTolerance;
    }


    public SystemStatus checkAndRecord(String cowperId, Instant currentTimestamp, Double currentDomeTemp) {
        if (cowperId == null || cowperId.isBlank() || currentTimestamp == null || currentDomeTemp == null) {
            return SystemStatus.INSUFFICIENT_DATA;
        }

        AtomicReference<SystemStatus> result = new AtomicReference<>(SystemStatus.ACTIVE);

        states.compute(cowperId, (id, prev) -> {
            if (prev == null) {
                return new StoveState(currentTimestamp, currentDomeTemp, currentTimestamp);
            }

            // Игнорируем устаревшие пакеты, пришедшие не по порядку
            if (currentTimestamp.isBefore(prev.lastTelemetryTimestamp())) {
                result.set(SystemStatus.INSUFFICIENT_DATA);
                return prev;
            }

            // Проверка пропуска данных между последовательными пакетами
            long gapMillis = Duration.between(prev.lastTelemetryTimestamp(), currentTimestamp).toMillis();
            boolean hasGap = gapMillis > staleThresholdMillis;

            // Проверка застывших показаний датчика купола
            boolean isSameTemp = Math.abs(currentDomeTemp - prev.lastDomeTemp()) <= frozenTolerance;
            Instant frozenSince = isSameTemp ? prev.frozenSinceTimestamp() : currentTimestamp;
            long frozenMillis = Duration.between(frozenSince, currentTimestamp).toMillis();
            boolean isFrozen = frozenMillis > frozenThresholdMillis;

            if (hasGap || isFrozen) {
                result.set(SystemStatus.INSUFFICIENT_DATA);
            }

            return new StoveState(currentTimestamp, currentDomeTemp, frozenSince);
        });

        return result.get();
    }

    public SystemStatus getCowperStatus(String cowperId) {
        if (cowperId == null || cowperId.isBlank()) {
            return SystemStatus.INSUFFICIENT_DATA;
        }
        StoveState state = states.get(cowperId);
        if (state == null) {
            return SystemStatus.INSUFFICIENT_DATA;
        }
        return isStateHealthy(state, Instant.now(clock))
            ? SystemStatus.ACTIVE
            : SystemStatus.INSUFFICIENT_DATA;
    }
    public SystemStatus getSystemStatus() {
        if (states.isEmpty()) {
            return SystemStatus.INSUFFICIENT_DATA;
        }
        Instant now = Instant.now(clock);
        boolean allHealthy = states.values().stream()
            .allMatch(state -> isStateHealthy(state, now));
        return allHealthy ? SystemStatus.ACTIVE : SystemStatus.INSUFFICIENT_DATA;
    }

    public void reset() {
        states.clear();
    }

    public void reset(String cowperId) {
        if (cowperId != null) {
            states.remove(cowperId);
        }
    }

    private boolean isStateHealthy(StoveState state, Instant now) {
        long ageMillis = Duration.between(state.lastTelemetryTimestamp(), now).toMillis();
        long frozenMillis = Duration.between(state.frozenSinceTimestamp(), state.lastTelemetryTimestamp()).toMillis();
        return ageMillis <= staleThresholdMillis && frozenMillis <= frozenThresholdMillis;
    }

}
