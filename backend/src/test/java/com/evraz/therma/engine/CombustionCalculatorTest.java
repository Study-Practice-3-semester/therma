package com.evraz.therma.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombustionCalculatorTest {

    private final CombustionCalculator calc = new CombustionCalculator();

    @Test
    void shouldCalculateRequiredGasFlowInCubicMetersPerHour() {
        double gasFlow = calc.calculateRequiredGasFlow(4000.0, 1150.0, 180.0, 4.19);

        assertEquals(36328.25, gasFlow, 0.1);
    }

    @Test
    void shouldReturnZeroGasFlowWhenNoHeatingRequired() {
        assertEquals(0.0, calc.calculateRequiredGasFlow(4000.0, 180.0, 180.0, 4.19));
        assertEquals(0.0, calc.calculateRequiredGasFlow(4000.0, 150.0, 180.0, 4.19));
    }

    @Test
    void shouldCalculateStoichiometryForThreePercentNaturalGas() {
        double qMix = calc.calculateMixedGasCalorific(3.0);
        double l0Mix = calc.calculateL0Mixed(3.0);
        double alpha = calc.calculateAlpha(0.973, l0Mix);
        double flueO2 = calc.calculateFlueO2(l0Mix, 1.10);

        assertEquals(4.185, qMix, 0.01);
        assertEquals(0.886, l0Mix, 0.005);
        assertEquals(1.098, alpha, 0.01);
        assertEquals(0.94, flueO2, 0.05);
    }

    @Test
    void shouldResolveL0FromCalorificWhenEnrichmentMissing() {
        double l0FromEnrichment = calc.resolveL0Mixed(3.0, null);
        double l0FromCalorific = calc.resolveL0Mixed(null, 4.1847);

        assertEquals(l0FromEnrichment, l0FromCalorific, 0.001);
    }
}
