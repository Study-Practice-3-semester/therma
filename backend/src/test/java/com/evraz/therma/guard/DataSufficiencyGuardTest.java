package com.evraz.therma.guard;

import com.evraz.therma.dto.SystemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DataSufficiencyGuardTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-23T10:00:00Z");
    private DataSufficiencyGuard guard;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(BASE_TIME.plus(Duration.ofMinutes(15)), ZoneOffset.UTC);
        guard = new DataSufficiencyGuard(fixedClock, 3, 10, 0.05);
    }

    @Test
    void shouldReturnInsufficientDataBeforeAnyTelemetryArrives() {
        assertEquals(SystemStatus.INSUFFICIENT_DATA, guard.getSystemStatus());
        assertEquals(SystemStatus.INSUFFICIENT_DATA, guard.getCowperStatus("VN-1"));
    }

    @Test
    void shouldDetectTelemetryGapBetweenPackets() {
        assertEquals(SystemStatus.ACTIVE, guard.checkAndRecord("VN-1", BASE_TIME, 1340.0));
        assertEquals(SystemStatus.ACTIVE, guard.checkAndRecord("VN-1", BASE_TIME.plus(Duration.ofMinutes(2)), 1342.0));

        SystemStatus afterGap = guard.checkAndRecord("VN-1", BASE_TIME.plus(Duration.ofMinutes(7)), 1345.0);
        assertEquals(SystemStatus.INSUFFICIENT_DATA, afterGap);
    }

    @Test
    void shouldDetectFrozenThermocoupleAndRecoverOnChange() {
        for (int min = 0; min <= 10; min++) {
            assertEquals(SystemStatus.ACTIVE,
                guard.checkAndRecord("VN-2", BASE_TIME.plus(Duration.ofMinutes(min)), 1312.0));
        }

        assertEquals(SystemStatus.INSUFFICIENT_DATA,
            guard.checkAndRecord("VN-2", BASE_TIME.plus(Duration.ofMinutes(11)), 1312.02));

        assertEquals(SystemStatus.ACTIVE,
            guard.checkAndRecord("VN-2", BASE_TIME.plus(Duration.ofMinutes(12)), 1264.0));
    }

    @Test
    void shouldIsolateStateBetweenDifferentCowpers() {
        Instant t14 = BASE_TIME.plus(Duration.ofMinutes(14));
        guard.checkAndRecord("VN-1", t14, 1350.0);
        guard.checkAndRecord("VN-2", BASE_TIME, 1310.0);

        assertEquals(SystemStatus.ACTIVE, guard.getCowperStatus("VN-1"));
        assertEquals(SystemStatus.INSUFFICIENT_DATA, guard.getCowperStatus("VN-2"));
        assertEquals(SystemStatus.INSUFFICIENT_DATA, guard.getSystemStatus());
    }
}
