package com.apisentinel.scanner;

import com.apisentinel.entity.FindingSeverity;

import java.util.Collection;
import java.util.Comparator;

/**
 * Risk score from 0 to 100. The most severe finding decides the band, and every finding
 * then adds a little within that band, so no number of medium findings can outrank a
 * single critical one. V5__recalculate_risk_scores.sql applies the same formula in SQL.
 */
public final class RiskScore {

    private RiskScore() {
    }

    public static int calculate(Collection<FindingSeverity> severities) {
        if (severities.isEmpty()) {
            return 0;
        }

        // FindingSeverity is declared from least to most severe, so the natural maximum is the worst.
        FindingSeverity highest = severities.stream().max(Comparator.naturalOrder()).orElseThrow();
        int weight = severities.stream().mapToInt(RiskScore::weight).sum();
        // Integer division rounds (weight / 2) half up, matching the SQL migration exactly.
        int withinBand = (weight + 1) / 2;
        return Math.min(bandMax(highest), bandMin(highest) + withinBand);
    }

    private static int bandMin(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 75;
            case HIGH -> 50;
            case MEDIUM -> 25;
            case LOW -> 5;
        };
    }

    private static int bandMax(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 100;
            case HIGH -> 74;
            case MEDIUM -> 49;
            case LOW -> 24;
        };
    }

    private static int weight(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 8;
            case HIGH -> 4;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
