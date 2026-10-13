-- PROCESSING was removed from ScanStatus in Milestone 6.
ALTER TABLE api_scans DROP CONSTRAINT api_scans_status_check;
ALTER TABLE api_scans ADD CONSTRAINT api_scans_status_check
    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'));

-- Scans are listed per project and findings per scan.
CREATE INDEX idx_api_scans_project_id ON api_scans (project_id);
CREATE INDEX idx_findings_scan_id ON findings (scan_id);
