package com.evraz.therma.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TelemetryRecord {
    private String cowperId;
    private Instant recordedAt;
    private Phase phase;

    // Температуры
    private Double hotBlastTemp;       // температура горячего дутья после смешения
    private Double hotBlastSetpoint;   // установка горячего дутья
    private Double coldBlastTemp;      // температура холодного дутья
    private Double domeTemp;           // температура купола
    private Double wasteGasTemp;       // температура дымовых газов
    private Double stoveOutletTemp;    // температура на выходе ВН (до смешения)

    // Расходы
    private Double mixedGasFlow;       // расход смешанного газа (м³/ч)
    private Double bfGasFlow;          // расход доменного газа (м³/ч)
    private Double natGasFlow;         // расход природного газа (м³/ч)
    private Double airFlow;            // расход воздуха горения (м³/ч)
    private Double combustionAirFlow;  // расход воздуха горения (альтернативное поле)
    private Double blastFlow;          // расход холодного дутья (м³/мин)

    // Расчётные и контроль
    private Double airGasRatio;        // соотношение воздух/газ (α · L0)
    private Double gasCalorific;       // теплота сгорания газа (МДж/м³)
    private Double flueO2;             // O2 в дымах (%)
    private Double flueCO;             // CO в дымах (ppm)
    private Double mixingColdShare;    // доля холодного воздуха через смеситель (%)
    private Double enrichmentNg;       // доля природного газа в смеси (%)
    private Double gasPressure;        // давление газа перед горелкой (кПа)
}
