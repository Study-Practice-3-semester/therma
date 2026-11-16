package com.evraz.therma.detector;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class TrendAnalyzer {

    public static class DataPoint {
        private final Instant timestamp;
        private final double value;

        public DataPoint(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public Instant getTimestamp() { return timestamp; }
        public double getValue() { return value; }
    }

    public double calculateRateOfChange(List<DataPoint> points) {
        if (points == null || points.size() < 2) {
            return 0.0;
        }

        DataPoint first = points.get(0);
        DataPoint last = points.get(points.size() - 1);

        long minutes = ChronoUnit.MINUTES.between(first.getTimestamp(), last.getTimestamp());
        if (minutes <= 0) {
            return 0.0;
        }

        return (last.getValue() - first.getValue()) / minutes;
    }

    public boolean isFrozen(List<DataPoint> points, double tolerance, int thresholdMinutes) {
        if (points == null || points.size() < 2) {
            return false;
        }

        DataPoint last = points.get(points.size() - 1);
        int frozenMinutes = 0;

        for (int i = points.size() - 2; i >= 0; i--) {
            DataPoint prev = points.get(i);
            if (Math.abs(last.getValue() - prev.getValue()) <= tolerance) {
                long mins = ChronoUnit.MINUTES.between(prev.getTimestamp(), last.getTimestamp());
                frozenMinutes = (int) mins;
            } else {
                break;
            }
        }

        return frozenMinutes >= thresholdMinutes;
    }

    public boolean hasSpike(List<DataPoint> points, double thresholdCelsiusPerMin) {
        if (points == null || points.size() < 2) {
            return false;
        }

        double rate = Math.abs(calculateRateOfChange(points));
        return rate > thresholdCelsiusPerMin;
    }

    public double forecast(List<DataPoint> points, int minutesAhead) {
        if (points == null || points.size() < 2) {
            return points != null && !points.isEmpty() ? points.get(points.size() - 1).getValue() : 0;
        }

        double rate = calculateRateOfChange(points);
        DataPoint last = points.get(points.size() - 1);
        return last.getValue() + rate * minutesAhead;
    }
}
