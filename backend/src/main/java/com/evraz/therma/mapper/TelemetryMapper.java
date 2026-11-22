package com.evraz.therma.mapper;

import com.evraz.therma.dto.TelemetryPoint;
import com.evraz.therma.dto.TelemetryRecord;
import org.springframework.stereotype.Component;

@Component
public class TelemetryMapper {

    public TelemetryRecord toRecord(TelemetryPoint point) {
        if (point == null) {
            return null;
        }

        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId(point.cowperId());
        record.setRecordedAt(point.recordedAt());
        record.setPhase(point.phase());

        record.setBlastFlow(point.blastFlow());
        record.setColdBlastTemp(point.coldBlastTemp());
        record.setHotBlastSetpoint(point.hotBlastSetpoint());
        record.setHotBlastTemp(point.hotBlastTemp());
        record.setMixingColdShare(point.mixingColdShare());
        record.setStoveOutletTemp(point.stoveOutletTemp());

        record.setBfGasFlow(point.bfGasFlow());
        record.setNatGasFlow(point.natGasFlow());
        record.setGasCalorific(point.gasCalorific());
        record.setEnrichmentNg(point.enrichmentNg());
        record.setGasPressure(point.gasPressure());

        record.setAirFlow(point.combustionAirFlow());
        record.setCombustionAirFlow(point.combustionAirFlow());

        record.setDomeTemp(point.domeTemp());
        record.setWasteGasTemp(point.flueGasTemp());
        record.setFlueO2(point.flueO2());
        record.setFlueCO(point.flueCO());

        // Расход смешанного газа: берём явно заданный либо суммируем ДГ + ПГ
        Double mixedGas = point.mixedGasFlow();
        if (mixedGas == null && (point.bfGasFlow() != null || point.natGasFlow() != null)) {
            double bf = point.bfGasFlow() != null ? point.bfGasFlow() : 0.0;
            double nat = point.natGasFlow() != null ? point.natGasFlow() : 0.0;
            mixedGas = bf + nat;
        }
        record.setMixedGasFlow(mixedGas);

        // Соотношение воздух/газ: берём явно заданное либо вычисляем из расходов
        if (point.airGasRatio() != null) {
            record.setAirGasRatio(point.airGasRatio());
        } else if (point.combustionAirFlow() != null && mixedGas != null && mixedGas > 0) {
            record.setAirGasRatio(point.combustionAirFlow() / mixedGas);
        }

        return record;
    }
}
