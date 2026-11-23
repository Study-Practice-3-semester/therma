package com.evraz.therma.dto;

import java.time.Instant;

public record TelemetryPoint(
    String cowperId,
    Instant recordedAt,
    Phase phase,
    Double blastFlow,
    Double coldBlastTemp,
    Double hotBlastSetpoint,
    Double hotBlastTemp,
    Double mixingColdShare,
    Double stoveOutletTemp,
    Double mixedGasFlow,
    Double bfGasFlow,
    Double natGasFlow,
    Double gasCalorific,
    Double enrichmentNg,
    Double gasPressure,
    Double combustionAirFlow,
    Double airGasRatio,
    Double domeTemp,
    Double flueGasTemp,
    Double flueO2,
    Double flueCO
) {}
