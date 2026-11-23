package com.evraz.therma;

import com.evraz.therma.dto.Phase;
import com.evraz.therma.dto.Recommendation;
import com.evraz.therma.dto.SystemStatus;
import com.evraz.therma.dto.TelemetryPoint;
import com.evraz.therma.guard.DataSufficiencyGuard;
import com.evraz.therma.service.RecommendationService;
import com.evraz.therma.service.TelemetryHistoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RecommendationPipelineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private DataSufficiencyGuard guard;

    @Autowired
    private TelemetryHistoryProvider historyProvider;

    @BeforeEach
    void cleanState() {
        guard.reset();
        historyProvider.clear("VN-1");
    }

    @Test
    void shouldExecuteAcceptanceDemoScenarioEndToEnd() {
        Instant t0 = Instant.now().minus(Duration.ofMinutes(2));

        // 1. Штатный разогрев (1380 - 1386 C): система ACTIVE
        assertTrue(recommendationService.processTelemetry(point("VN-1", t0, 1355.0)).isEmpty());
        assertTrue(recommendationService.processTelemetry(point("VN-1", t0.plus(Duration.ofMinutes(1)), 1365.0)).isEmpty());
        assertEquals(SystemStatus.ACTIVE, recommendationService.getCowperStatus("VN-1"));

        // 2. Уход температуры купола выше 1390 C с ростом - срабатывает R1 с объяснением
        Instant t2 = t0.plus(Duration.ofMinutes(2));
        List<Recommendation> overheatRecs = recommendationService.processTelemetry(
            point("VN-1", t2, 1394.0)
        );
        assertFalse(overheatRecs.isEmpty());
        Recommendation r1 = overheatRecs.getFirst();
        assertEquals("R1_DOME_GROWING", r1.ruleCode());
        assertEquals("air_gas_ratio", r1.targetParameter());
        assertFalse(r1.reasoning().isEmpty());

        // 3. Обрыв связи на 5 минут (> порога 3 мин) - INSUFFICIENT_DATA
        List<Recommendation> gapRecs = recommendationService.processTelemetry(
            point("VN-1", t2.plus(Duration.ofMinutes(5)), 1396.0)
        );
        assertEquals(1, gapRecs.size());
        assertEquals("R7_INSUFFICIENT_DATA", gapRecs.getFirst().ruleCode());
    }

    private TelemetryPoint point(String cowperId, Instant ts, double domeTemp) {
        return new TelemetryPoint(
            cowperId, ts, Phase.HEATING,
            4000.0, 180.0, 1150.0, 1150.0, 8.0, 1250.0,
            46000.0, 44620.0, 1380.0, 4.2, 3.0, 8.1,
            44800.0, 0.973, domeTemp, 290.0, 1.0, 35.0
        );
    }
}
