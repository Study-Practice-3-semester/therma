package com.evraz.therma.detector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class TrendAnalyzer {
    private static final double MIN_SPIKE_DELTA_CELSIUS = 15.0;

    @Getter
    @RequiredArgsConstructor
    public static class DataPoint {
        private final Instant timestamp;
        private final double value;
    }

    public double calculateRateOfChange(List<DataPoint> points) {
        if (points == null || points.size() < 2) {
            return 0.0;
        }

        DataPoint first = points.get(0);
        DataPoint last = points.get(points.size() - 1);

        double minutes = Duration.between(first.getTimestamp(), last.getTimestamp()).toMillis() / 60_000.0;
        if (minutes <= 0.0) {
            return 0.0;
        }

        return (last.getValue() - first.getValue()) / minutes;
    }

    public boolean isFrozen(List<DataPoint> points, double tolerance, int thresholdMinutes) {
        if (points == null || points.size() < 2) {
            return false;
        }

        DataPoint last = points.get(points.size() - 1);
        double frozenMinutes = 0.0;

        for (int i = points.size() - 2; i >= 0; i--) {
            DataPoint prev = points.get(i);
            if (Math.abs(last.getValue() - prev.getValue()) <= tolerance) {
                frozenMinutes = Duration.between(prev.getTimestamp(), last.getTimestamp()).toMillis() / 60_000.0;
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

        for (int i = 1; i < points.size(); i++) {
            DataPoint prev = points.get(i - 1);
            DataPoint curr = points.get(i);

            double delta = Math.abs(curr.getValue() - prev.getValue());
            if (delta < MIN_SPIKE_DELTA_CELSIUS) {
                continue;
            }

            double minutes = Duration.between(prev.getTimestamp(), curr.getTimestamp()).toMillis() / 60_000.0;
            if (minutes <= 0.0) {
                continue;
            }

            if (delta / minutes > thresholdCelsiusPerMin) {
                return true;
            }
        }

        return false;
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
