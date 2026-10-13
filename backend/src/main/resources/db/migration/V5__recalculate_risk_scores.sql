-- Risk scores now come from the most severe finding plus a little per finding
-- (see RiskScore.java). Recalculate completed scans so history and comparisons
-- use one formula.
UPDATE api_scans
SET risk_score = (
    SELECT CASE MAX(CASE f.severity
                        WHEN 'CRITICAL' THEN 4
                        WHEN 'HIGH' THEN 3
                        WHEN 'MEDIUM' THEN 2
                        ELSE 1
                    END)
        WHEN 4 THEN LEAST(100, 75 + (SUM(CASE f.severity WHEN 'CRITICAL' THEN 8 WHEN 'HIGH' THEN 4 WHEN 'MEDIUM' THEN 2 ELSE 1 END) + 1) / 2)
        WHEN 3 THEN LEAST(74, 50 + (SUM(CASE f.severity WHEN 'CRITICAL' THEN 8 WHEN 'HIGH' THEN 4 WHEN 'MEDIUM' THEN 2 ELSE 1 END) + 1) / 2)
        WHEN 2 THEN LEAST(49, 25 + (SUM(CASE f.severity WHEN 'CRITICAL' THEN 8 WHEN 'HIGH' THEN 4 WHEN 'MEDIUM' THEN 2 ELSE 1 END) + 1) / 2)
        WHEN 1 THEN LEAST(24, 5 + (SUM(CASE f.severity WHEN 'CRITICAL' THEN 8 WHEN 'HIGH' THEN 4 WHEN 'MEDIUM' THEN 2 ELSE 1 END) + 1) / 2)
        ELSE 0
    END
    FROM findings f
    WHERE f.scan_id = api_scans.id
)
WHERE status = 'COMPLETED';
