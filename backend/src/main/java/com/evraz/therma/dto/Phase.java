package com.evraz.therma.dto;

public enum Phase {
    HEATING,      // на газе (нагрев)
    BLOWING,      // на дутье
    CHANGING,     // перекидка
    ISOLATED,     // изолирован
    RESERVE,      // резерв
    MAINTENANCE   // обслуживание
}
