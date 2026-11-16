package com.evraz.therma.dto;

import java.time.Instant;

public class TelemetryPoint {
    private String cowperId;
    private Instant recordedAt;
    private Phase phase;
    private Double hotBlastTemp;
    private Double domeTemp;
    private Double flueGasTemp;
    private Double bfGasFlow;
    private Double natGasFlow;
    private Double combustionAirFlow;

    public String getCowperId() { return cowperId; }
    public void setCowperId(String cowperId) { this.cowperId = cowperId; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public Double getHotBlastTemp() { return hotBlastTemp; }
    public void setHotBlastTemp(Double hotBlastTemp) { this.hotBlastTemp = hotBlastTemp; }

    public Double getDomeTemp() { return domeTemp; }
    public void setDomeTemp(Double domeTemp) { this.domeTemp = domeTemp; }

    public Double getFlueGasTemp() { return flueGasTemp; }
    public void setFlueGasTemp(Double flueGasTemp) { this.flueGasTemp = flueGasTemp; }

    public Double getBfGasFlow() { return bfGasFlow; }
    public void setBfGasFlow(Double bfGasFlow) { this.bfGasFlow = bfGasFlow; }

    public Double getNatGasFlow() { return natGasFlow; }
    public void setNatGasFlow(Double natGasFlow) { this.natGasFlow = natGasFlow; }

    public Double getCombustionAirFlow() { return combustionAirFlow; }
    public void setCombustionAirFlow(Double combustionAirFlow) { this.combustionAirFlow = combustionAirFlow; }
}
