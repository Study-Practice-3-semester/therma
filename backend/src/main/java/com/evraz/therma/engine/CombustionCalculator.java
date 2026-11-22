package com.evraz.therma.engine;

import org.springframework.stereotype.Component;

@Component
public class CombustionCalculator {

    // Типовые константы
    private static final double DEFAULT_ENRICHMENT_NG_PCT = 3.0;
    private static final double C_AIR = 1.36;          // кДж/(м³·К), теплоёмкость воздуха
    private static final double C_PS = 1.52;           // кДж/(м³·К), теплоёмкость продуктов сгорания
    private static final double ETA_STOVE = 0.78;      // КПД воздухонагревателя
    private static final double ETA_PYR = 0.92;        // пирометрический коэффициент
    private static final double L0_DG = 0.62;          // м³/м³, теор. воздух для доменного газа
    private static final double L0_NG = 9.5;           // м³/м³, теор. воздух для природного газа
    private static final double Q_DG = 3.21;           // МДж/м³, теплота доменного газа
    private static final double Q_NG = 35.7;           // МДж/м³, теплота природного газа

    public double calculateMixedGasCalorific(Double enrichmentNgPct) {
        if (enrichmentNgPct == null) {
            return Q_DG;
        }
        double x = enrichmentNgPct / 100.0;
        return x * Q_NG + (1 - x) * Q_DG;
    }

    public double resolveL0Mixed(Double enrichmentNgPct, Double gasCalorific) {
        if (enrichmentNgPct != null) {
            return calculateL0Mixed(enrichmentNgPct);
        }
        if (gasCalorific != null && gasCalorific > Q_DG) {
            double x = Math.clamp((gasCalorific - Q_DG) / (Q_NG - Q_DG), 0.0, 1.0);
            return x * L0_NG + (1.0 - x) * L0_DG;
        }
        return calculateL0Mixed(DEFAULT_ENRICHMENT_NG_PCT);
    }

    public double calculateL0Mixed(Double enrichmentNgPct) {
        if (enrichmentNgPct == null) {
            return L0_DG;
        }
        double x = enrichmentNgPct / 100.0;
        return x * L0_NG + (1 - x) * L0_DG;
    }

    public double calculateAdiabaticTemperature(double qMixed, double l0Mixed, double alpha) {
        double vPs = 1.0 + alpha * l0Mixed;
        double qKj = qMixed * 1000.0;  // МДж → кДж
        return qKj / (vPs * C_PS);
    }

    public double calculateDomeTemperature(double qMixed, double l0Mixed, double alpha) {
        return ETA_PYR * calculateAdiabaticTemperature(qMixed, l0Mixed, alpha);
    }

    public double calculateFlueO2(double l0Mixed, double alpha) {
        double vPs = 1.0 + alpha * l0Mixed;
        return 21.0 * (alpha - 1.0) * l0Mixed / vPs;
    }

    public double calculateAirGasRatio(double l0Mixed, double alpha) {
        return alpha * l0Mixed;
    }

    public double calculateRequiredGasFlow(
        double blastFlow,
        double hotBlastTemp,
        double coldBlastTemp,
        double qMixed,
        double tauDut,
        double tauGas) {
        if (qMixed <= 0 || tauGas <= 0) {
            throw new IllegalArgumentException("Теплота сгорания (qMixed) и длительность нагрева (tauGas) должны быть > 0");
        }
        if (hotBlastTemp <= coldBlastTemp) {
            return 0.0;
        }

        double blastFlowM3PerHour = blastFlow * 60.0;   // м³/мин -> м³/ч
        double qMixedKjPerM3 = qMixed * 1000.0;         // МДж/м³ -> кДж/м³

        double numerator = blastFlowM3PerHour * C_AIR * (hotBlastTemp - coldBlastTemp) * tauDut;
        double denominator = qMixedKjPerM3 * ETA_STOVE * tauGas;
        return numerator / denominator;
    }

    public double calculateRequiredGasFlow(
        double blastFlow,
        double hotBlastTemp,
        double coldBlastTemp,
        double qMixed) {
        return calculateRequiredGasFlow(blastFlow, hotBlastTemp, coldBlastTemp, qMixed, 60.0, 160.0);
    }

    public double calculateMixingColdShare(
        double stoveOutletTemp,
        double hotBlastSetpoint,
        double coldBlastTemp) {
        if (stoveOutletTemp <= hotBlastSetpoint) {
            return 0.0;
        }
        double denominator = stoveOutletTemp - coldBlastTemp;
        if (denominator <= 0) {
            return 0.0;
        }
        return (stoveOutletTemp - hotBlastSetpoint) / denominator;
    }

    public double calculateAlpha(double airGasRatio, double l0Mixed) {
        if (l0Mixed <= 0) {
            return 1.0;
        }
        return airGasRatio / l0Mixed;
    }
}
