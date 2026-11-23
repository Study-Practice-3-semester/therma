package com.evraz.therma.service;

import com.evraz.therma.detector.TrendAnalyzer.DataPoint;
import com.evraz.therma.dto.Phase;
import com.evraz.therma.dto.TelemetryRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTelemetryHistoryBuffer implements TelemetryHistoryProvider {

    private static final int MAX_POINTS_PER_COWPER = 1200;
    private static final Duration MAX_RETENTION = Duration.ofMinutes(20);

    private static final class CowperBuffer {
        private Phase lastPhase;
        private final Deque<DataPoint> domePoints = new ArrayDeque<>();
        private final Deque<DataPoint> wastePoints = new ArrayDeque<>();
    }

    private final Map<String, CowperBuffer> buffers = new ConcurrentHashMap<>();

    @Override
    public void record(TelemetryRecord record) {
        if (record == null || record.getCowperId() == null || record.getRecordedAt() == null) {
            return;
        }

        CowperBuffer buffer = buffers.computeIfAbsent(record.getCowperId(), id -> new CowperBuffer());
        synchronized (buffer) {
            Phase currentPhase = record.getPhase();
            if (buffer.lastPhase != null && currentPhase != null && buffer.lastPhase != currentPhase) {
                buffer.domePoints.clear();
                buffer.wastePoints.clear();
            }
            if (currentPhase != null) {
                buffer.lastPhase = currentPhase;
            }

            Instant ts = record.getRecordedAt();
            if (record.getDomeTemp() != null) {
                appendPoint(buffer.domePoints, new DataPoint(ts, record.getDomeTemp()), ts);
            }
            if (record.getWasteGasTemp() != null) {
                appendPoint(buffer.wastePoints, new DataPoint(ts, record.getWasteGasTemp()), ts);
            }
        }
    }

    @Override
    public List<DataPoint> getDomeTempHistory(String cowperId, Duration window) {
        return snapshot(cowperId, window, true);
    }

    @Override
    public List<DataPoint> getWasteGasTempHistory(String cowperId, Duration window) {
        return snapshot(cowperId, window, false);
    }

    @Override
    public void clear(String cowperId) {
        if (cowperId != null) {
            buffers.remove(cowperId);
        }
    }

    private void appendPoint(Deque<DataPoint> deque, DataPoint point, Instant now) {
        if (!deque.isEmpty() && !point.getTimestamp().isAfter(deque.peekLast().getTimestamp())) {
            return;
        }

        deque.addLast(point);
        Instant cutoff = now.minus(MAX_RETENTION);
        while (!deque.isEmpty() && deque.peekFirst().getTimestamp().isBefore(cutoff)) {
            deque.removeFirst();
        }
        while (deque.size() > MAX_POINTS_PER_COWPER) {
            deque.removeFirst();
        }
    }

    private List<DataPoint> snapshot(String cowperId, Duration window, boolean dome) {
        if (cowperId == null) {
            return List.of();
        }
        CowperBuffer buffer = buffers.get(cowperId);
        if (buffer == null) {
            return List.of();
        }

        synchronized (buffer) {
            Deque<DataPoint> source = dome ? buffer.domePoints : buffer.wastePoints;
            if (source.isEmpty()) {
                return List.of();
            }

            Instant latest = source.peekLast().getTimestamp();
            Instant from = latest.minus(window);

            List<DataPoint> result = new ArrayList<>(source.size());
            for (DataPoint p : source) {
                if (!p.getTimestamp().isBefore(from)) {
                    result.add(p);
                }
            }
            return result;
        }
    }
}
