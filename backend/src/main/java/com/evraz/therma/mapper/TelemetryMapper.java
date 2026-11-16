package com.evraz.therma.mapper;

import com.evraz.therma.dto.*;
import org.springframework.stereotype.Component;

/**
 * Маппер между внешними форматами телеметрии (REST, STOMP)
 * и внутренним унифицированным форматом TelemetryRecord.
 */
@Component
public class TelemetryMapper {

    /**
     * Конвертация из REST-формата (TelemetryPoint) во внутренний формат.
     */
    public TelemetryRecord fromRest(TelemetryPoint point) {
        if (point == null) {
            return null;
        }

        TelemetryRecord record = new TelemetryRecord();
        record.setCowperId(point.getCowperId());
        record.setRecordedAt(point.getRecordedAt());
        record.setPhase(point.getPhase());
        record.setDomeTemp(point.getDomeTemp());
        record.setHotBlastTemp(point.getHotBlastTemp());
        record.setWasteGasTemp(point.getFlueGasTemp());
        record.setBfGasFlow(point.getBfGasFlow());
        record.setNatGasFlow(point.getNatGasFlow());
        record.setCombustionAirFlow(point.getCombustionAirFlow());

        // Расчёт air_gas_ratio из REST-полей
        // TODO: согласовать формулу с командой
        if (point.getCombustionAirFlow() != null && point.getBfGasFlow() != null && point.getNatGasFlow() != null) {
            double totalGas = point.getBfGasFlow() + point.getNatGasFlow();
            if (totalGas > 0) {
                record.setAirGasRatio(point.getCombustionAirFlow() / totalGas);
            }
        }

        return record;
    }

    /**
     * Конвертация из STOMP-формата (TelemetryMessage) во внутренний формат.
     */
    public TelemetryRecord fromStomp(TelemetryMessage message) {
        if (message == null) {
            return null;
        }

        TelemetryRecord record = new TelemetryRecord();
        // TODO: cowperId и phase нужно брать из контекста подписки
        record.setRecordedAt(message.getRecordedAt());
        record.setDomeTemp(message.getDomeTemp());
        record.setHotBlastTemp(message.getHotBlastTemp());
        record.setWasteGasTemp(message.getFlueGasTemp());
        record.setAirFlow(message.getAirFlow());
        record.setCombustionAirFlow(message.getCombustionAirFlow());

        // В STOMP gasFlow - это сумма bfGasFlow + natGasFlow
        // TODO: уточнить у команды, как разделять
        if (message.getGasFlow() != null) {
            record.setMixedGasFlow(message.getGasFlow());
        }

        // Расчёт air_gas_ratio из STOMP-полей
        if (message.getAirFlow() != null && message.getGasFlow() != null && message.getGasFlow() > 0) {
            record.setAirGasRatio(message.getAirFlow() / message.getGasFlow());
        }

        return record;
    }
}
