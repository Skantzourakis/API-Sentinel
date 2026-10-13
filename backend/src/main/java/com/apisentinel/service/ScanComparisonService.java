package com.apisentinel.service;

import com.apisentinel.dto.finding.FindingResponse;
import com.apisentinel.dto.scan.ScanComparisonResponse;
import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.ScanStatus;
import com.apisentinel.exception.BadRequestException;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ApiScanRepository;
import com.apisentinel.repository.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScanComparisonService {

    // The same rule on the same endpoint is treated as the same issue across scans.
    private record FindingKey(String ruleId, String method, String path) {

        static FindingKey of(Finding finding) {
            return new FindingKey(finding.getRuleId(), finding.getMethod(), finding.getPath());
        }
    }

    private final ApiScanRepository apiScanRepository;
    private final FindingRepository findingRepository;

    public ScanComparisonService(ApiScanRepository apiScanRepository, FindingRepository findingRepository) {
        this.apiScanRepository = apiScanRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional(readOnly = true)
    public ScanComparisonResponse compare(Long ownerId, Long scanId, Long baseScanId) {
        ApiScan scan = findCompletedScan(ownerId, scanId);
        ApiScan baseScan = baseScanId != null
                ? findCompletedScan(ownerId, baseScanId)
                : findPreviousCompletedScan(scan);

        if (!baseScan.getProject().getId().equals(scan.getProject().getId())) {
            throw new BadRequestException("Scans " + scanId + " and " + baseScan.getId() + " belong to different projects");
        }

        Map<FindingKey, Finding> current = findingsByKey(scan.getId());
        Map<FindingKey, Finding> previous = findingsByKey(baseScan.getId());

        List<FindingResponse> newFindings = current.entrySet().stream()
                .filter(entry -> !previous.containsKey(entry.getKey()))
                .map(entry -> FindingResponse.from(entry.getValue()))
                .toList();
        List<FindingResponse> resolvedFindings = previous.entrySet().stream()
                .filter(entry -> !current.containsKey(entry.getKey()))
                .map(entry -> FindingResponse.from(entry.getValue()))
                .toList();
        int unchangedFindings = current.size() - newFindings.size();

        return ScanComparisonResponse.from(scan, baseScan, newFindings, resolvedFindings, unchangedFindings);
    }

    private ApiScan findCompletedScan(Long ownerId, Long scanId) {
        ApiScan scan = apiScanRepository.findByIdAndProjectOwnerId(scanId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan with id " + scanId + " was not found"));
        // FAILED scans have no findings, so comparing with them would report everything as new or resolved.
        if (scan.getStatus() != ScanStatus.COMPLETED) {
            throw new BadRequestException("Scan " + scanId + " is not completed and cannot be compared");
        }
        return scan;
    }

    private ApiScan findPreviousCompletedScan(ApiScan scan) {
        return apiScanRepository
                .findFirstByProjectIdAndStatusAndIdLessThanOrderByIdDesc(scan.getProject().getId(), ScanStatus.COMPLETED, scan.getId())
                .orElseThrow(() -> new BadRequestException("Scan " + scan.getId() + " has no earlier completed scan to compare with"));
    }

    private Map<FindingKey, Finding> findingsByKey(Long scanId) {
        return findingRepository.findByScanIdOrderByIdAsc(scanId).stream()
                .collect(LinkedHashMap::new, (map, finding) -> map.putIfAbsent(FindingKey.of(finding), finding), Map::putAll);
    }
}
