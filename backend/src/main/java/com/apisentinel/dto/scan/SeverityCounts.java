package com.apisentinel.dto.scan;

import com.apisentinel.entity.FindingSeverity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SeverityCounts {

    private static final List<FindingSeverity> MOST_SEVERE_FIRST =
            List.of(FindingSeverity.CRITICAL, FindingSeverity.HIGH, FindingSeverity.MEDIUM, FindingSeverity.LOW);

    private SeverityCounts() {
    }

    // Every severity starts at 0 so missing ones still appear in responses, most severe first.
    public static Map<FindingSeverity, Long> zeroed() {
        Map<FindingSeverity, Long> counts = new LinkedHashMap<>();
        MOST_SEVERE_FIRST.forEach(severity -> counts.put(severity, 0L));
        return counts;
    }
}
