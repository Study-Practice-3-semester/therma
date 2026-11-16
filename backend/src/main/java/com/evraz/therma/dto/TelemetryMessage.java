package com.evraz.therma.dto;

import java.time.Instant;

public class TelemetryMessage {
    private String id;
    private Instant recordedAt;
    private String phase;
    private Double domeTemp;
    private Double hotBlastTemp;
    private Double flueGasTemp;
    private Double gasFlow;
    private Double airFlow;
    private Double combustionAirFlow;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public Double getDomeTemp() { return domeTemp; }
    public void setDomeTemp(Double domeTemp) { this.domeTemp = domeTemp; }

    public Double getHotBlastTemp() { return hotBlastTemp; }
    public void setHotBlastTemp(Double hotBlastTemp) { this.hotBlastTemp = hotBlastTemp; }

    public Double getFlueGasTemp() { return flueGasTemp; }
    public void setFlueGasTemp(Double flueGasTemp) { this.flueGasTemp = flueGasTemp; }

    public Double getGasFlow() { return gasFlow; }
    public void setGasFlow(Double gasFlow) { this.gasFlow = gasFlow; }

    public Double getAirFlow() { return airFlow; }
    public void setAirFlow(Double airFlow) { this.airFlow = airFlow; }

    public Double getCombustionAirFlow() { return combustionAirFlow; }
    public void setCombustionAirFlow(Double combustionAirFlow) { this.combustionAirFlow = combustionAirFlow; }
}
