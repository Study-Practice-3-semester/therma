package com.evraz.therma.guard;

import com.evraz.therma.dto.SystemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DataSufficiencyGuardTest {

    private DataSufficiencyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new DataSufficiencyGuard();
        // Устанавливаем пороги через рефлексию (в реальном проекте — через @Value)
        // Для теста используем значения по умолчанию: 3 мин stale, 10 мин frozen
    }

    @Test
    void shouldReturnActiveForFreshTelemetry() {
        // Дано: свежая телеметрия
        Instant now = Instant.now();

        // Когда
        SystemStatus status = guard.check(now, 1350.0);

        // Тогда
        assertEquals(SystemStatus.ACTIVE, status);
    }

    @Test
    void shouldReturnActiveForNullTimestampFirstTime() {
        // Дано: первый вызов с null timestamp
        // Когда
        SystemStatus status = guard.check(null, null);

        // Тогда
        assertEquals(SystemStatus.ACTIVE, status);
    }

    @Test
    void shouldResetState() {
        // Дано: guard в каком-то состоянии
        guard.check(Instant.now(), 1350.0);

        // Когда: сброс
        guard.reset();

        // Тогда: состояние очищено (следующий вызов с null вернёт ACTIVE)
        assertEquals(SystemStatus.ACTIVE, guard.check(null, null));
    }
}
