package com.evraz.therma.service;

import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.TelemetryRecord;

import java.time.Duration;
import java.util.List;

public interface TelemetryHistoryProvider {
    void record(TelemetryRecord record);

    List<DataPoint> getDomeTempHistory(String cowperId, Duration window);

    List<DataPoint> getWasteGasTempHistory(String cowperId, Duration window);

    void clear(String cowperId);
}
