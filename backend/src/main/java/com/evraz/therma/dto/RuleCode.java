package com.evraz.therma.dto;

public enum RuleCode {
    // Правила безопасности (критические инциденты)
    DHN_DOME_OVERHEAT,      // купол > 1390 °C
    WARN_DOME_APPROACH,     // купол приближается к 1350 °C
    DHN_WASTE_OVERHEAT,     // дымы > 350 °C

    // Правила оптимизации горения
    WARN_AIR_DEFICIT,       // воздух/газ < 1.1
    TWO_AIR_EXCESS,         // воздух/газ > 1.4

    // Правила технологического режима
    INFO_BLAST_TEMP_DROP,   // горячее дутьё ниже уставки
    SAFETY_GAS_PRESSURE,    // давление газа < 5 кПа (риск погасания)

    // Расширенные правила (R1-R7 из KB)
    R1_DOME_GROWING,        // купол растёт
    R2_WASTE_TREND,         // дымы растут к 400
    R3_INCOMPLETE_COMBUSTION, // CO > 1000 или O2 < 0.3
    R4_CALORIFIC_DROP,      // теплота газа упала
    R5_BLAST_UNDERHEAT,     // прогноз T_вых < уставки
    R6_O2_EXCESS,           // O2 > 3%
    R7_INSUFFICIENT_DATA    // нет данных
}
