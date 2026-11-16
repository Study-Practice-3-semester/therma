package com.evraz.therma.dto;

import java.time.Instant;

public class TelemetryRecord {
    private String cowperId;
    private Instant recordedAt;
    private Phase phase;

    // Температуры
    private Double hotBlastTemp;       // температура горячего дутья после смешения
    private Double hotBlastSetpoint;   // уставка горячего дутья
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

    // Расчётные
    private Double airGasRatio;        // соотношение воздух/газ (α · L0)
    private Double gasCalorific;       // теплота сгорания газа (МДж/м³)
    private Double flueO2;             // O2 в дымах (%)
    private Double flueCO;             // CO в дымах (ppm)
    private Double mixingColdShare;    // доля холодного воздуха через смеситель (%)
    private Double enrichmentNg;       // доля природного газа в смеси (%)

    // Getters and setters
    public String getCowperId() { return cowperId; }
    public void setCowperId(String cowperId) { this.cowperId = cowperId; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public Double getHotBlastTemp() { return hotBlastTemp; }
    public void setHotBlastTemp(Double hotBlastTemp) { this.hotBlastTemp = hotBlastTemp; }

    public Double getHotBlastSetpoint() { return hotBlastSetpoint; }
    public void setHotBlastSetpoint(Double hotBlastSetpoint) { this.hotBlastSetpoint = hotBlastSetpoint; }

    public Double getColdBlastTemp() { return coldBlastTemp; }
    public void setColdBlastTemp(Double coldBlastTemp) { this.coldBlastTemp = coldBlastTemp; }

    public Double getDomeTemp() { return domeTemp; }
    public void setDomeTemp(Double domeTemp) { this.domeTemp = domeTemp; }

    public Double getWasteGasTemp() { return wasteGasTemp; }
    public void setWasteGasTemp(Double wasteGasTemp) { this.wasteGasTemp = wasteGasTemp; }

    public Double getStoveOutletTemp() { return stoveOutletTemp; }
    public void setStoveOutletTemp(Double stoveOutletTemp) { this.stoveOutletTemp = stoveOutletTemp; }

    public Double getMixedGasFlow() { return mixedGasFlow; }
    public void setMixedGasFlow(Double mixedGasFlow) { this.mixedGasFlow = mixedGasFlow; }

    public Double getBfGasFlow() { return bfGasFlow; }
    public void setBfGasFlow(Double bfGasFlow) { this.bfGasFlow = bfGasFlow; }

    public Double getNatGasFlow() { return natGasFlow; }
    public void setNatGasFlow(Double natGasFlow) { this.natGasFlow = natGasFlow; }

    public Double getAirFlow() { return airFlow; }
    public void setAirFlow(Double airFlow) { this.airFlow = airFlow; }

    public Double getCombustionAirFlow() { return combustionAirFlow; }
    public void setCombustionAirFlow(Double combustionAirFlow) { this.combustionAirFlow = combustionAirFlow; }

    public Double getBlastFlow() { return blastFlow; }
    public void setBlastFlow(Double blastFlow) { this.blastFlow = blastFlow; }

    public Double getAirGasRatio() { return airGasRatio; }
    public void setAirGasRatio(Double airGasRatio) { this.airGasRatio = airGasRatio; }

    public Double getGasCalorific() { return gasCalorific; }
    public void setGasCalorific(Double gasCalorific) { this.gasCalorific = gasCalorific; }

    public Double getFlueO2() { return flueO2; }
    public void setFlueO2(Double flueO2) { this.flueO2 = flueO2; }

    public Double getFlueCO() { return flueCO; }
    public void setFlueCO(Double flueCO) { this.flueCO = flueCO; }

    public Double getMixingColdShare() { return mixingColdShare; }
    public void setMixingColdShare(Double mixingColdShare) { this.mixingColdShare = mixingColdShare; }

    public Double getEnrichmentNg() { return enrichmentNg; }
    public void setEnrichmentNg(Double enrichmentNg) { this.enrichmentNg = enrichmentNg; }
}
