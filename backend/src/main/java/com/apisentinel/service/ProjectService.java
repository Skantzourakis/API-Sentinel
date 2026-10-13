package com.apisentinel.service;

import com.apisentinel.dto.project.ProjectRequest;
import com.apisentinel.dto.project.ProjectResponse;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.dto.scan.SeverityCounts;
import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.entity.Project;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ApiScanRepository;
import com.apisentinel.repository.FindingRepository;
import com.apisentinel.repository.ProjectRepository;
import com.apisentinel.repository.ProjectScanCount;
import com.apisentinel.repository.ScanSeverityCount;
import com.apisentinel.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Used for business logic related to Project entity. It handles the processing of project data, including creating, retrieving, updating, and deleting projects. The service interacts with the ProjectRepository to perform database operations and converts between ProjectRequest and ProjectResponse DTOs for communication with the controller layer.
// Every method receives the id of the logged-in user and only touches that user's projects.
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApiScanRepository apiScanRepository;
    private final FindingRepository findingRepository;
    private final UserAccountRepository userAccountRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ApiScanRepository apiScanRepository,
            FindingRepository findingRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.projectRepository = projectRepository;
        this.apiScanRepository = apiScanRepository;
        this.findingRepository = findingRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ProjectResponse createProject(Long ownerId, ProjectRequest request) {
        Project project = new Project(
                request.getName().trim(),
                normalizeDescription(request.getDescription()),
                userAccountRepository.getReferenceById(ownerId)
        );
        Project savedProject = projectRepository.save(project);
        return ProjectResponse.from(savedProject, 0, null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(Long ownerId) {
        // Three queries in total, however many projects there are: scan counts, latest scans, their findings.
        Map<Long, Long> scanCounts = new HashMap<>();
        for (ProjectScanCount row : apiScanRepository.countScansPerProjectOfOwner(ownerId)) {
            scanCounts.put(row.getProjectId(), row.getCount());
        }

        List<ApiScan> latestScans = apiScanRepository.findLatestScanPerProjectOfOwner(ownerId);
        Map<Long, Map<FindingSeverity, Long>> countsByScan =
                severityCountsByScan(latestScans.stream().map(ApiScan::getId).toList());
        Map<Long, ApiScanResponse> latestByProject = new HashMap<>();
        for (ApiScan scan : latestScans) {
            latestByProject.put(scan.getProject().getId(),
                    ApiScanResponse.from(scan, countsByScan.getOrDefault(scan.getId(), SeverityCounts.zeroed())));
        }

        return projectRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(project -> ProjectResponse.from(
                        project,
                        scanCounts.getOrDefault(project.getId(), 0L),
                        latestByProject.get(project.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long ownerId, Long id) {
        return toResponse(findProject(ownerId, id));
    }

    @Transactional
    public ProjectResponse updateProject(Long ownerId, Long id, ProjectRequest request) {
        Project project = findProject(ownerId, id);
        project.setName(request.getName().trim());
        project.setDescription(normalizeDescription(request.getDescription()));
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(Long ownerId, Long id) {
        Project project = findProject(ownerId, id);
        findingRepository.deleteByScanProjectId(id);
        apiScanRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private ProjectResponse toResponse(Project project) {
        ApiScanResponse latestScan = apiScanRepository.findFirstByProjectIdOrderByIdDesc(project.getId())
                .map(scan -> ApiScanResponse.from(scan,
                        severityCountsByScan(List.of(scan.getId())).getOrDefault(scan.getId(), SeverityCounts.zeroed())))
                .orElse(null);
        return ProjectResponse.from(project, apiScanRepository.countByProjectId(project.getId()), latestScan);
    }

    private Map<Long, Map<FindingSeverity, Long>> severityCountsByScan(Collection<Long> scanIds) {
        Map<Long, Map<FindingSeverity, Long>> counts = new HashMap<>();
        if (scanIds.isEmpty()) {
            return counts;
        }
        for (ScanSeverityCount row : findingRepository.countBySeverityForScans(scanIds)) {
            counts.computeIfAbsent(row.getScanId(), id -> SeverityCounts.zeroed()).put(row.getSeverity(), row.getCount());
        }
        return counts;
    }

    // Another user's project gives 404, not 403, so its existence is not revealed.
    private Project findProject(Long ownerId, Long id) {
        return projectRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " was not found"));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
