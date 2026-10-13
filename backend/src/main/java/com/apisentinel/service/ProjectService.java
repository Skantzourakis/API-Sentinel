package com.apisentinel.service;

import com.apisentinel.dto.project.ProjectRequest;
import com.apisentinel.dto.project.ProjectResponse;
import com.apisentinel.entity.Project;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ApiScanRepository;
import com.apisentinel.repository.FindingRepository;
import com.apisentinel.repository.ProjectRepository;
import com.apisentinel.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(Long ownerId) {
        return projectRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long ownerId, Long id) {
        return ProjectResponse.from(findProject(ownerId, id));
    }

    @Transactional
    public ProjectResponse updateProject(Long ownerId, Long id, ProjectRequest request) {
        Project project = findProject(ownerId, id);
        project.setName(request.getName().trim());
        project.setDescription(normalizeDescription(request.getDescription()));
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long ownerId, Long id) {
        Project project = findProject(ownerId, id);
        findingRepository.deleteByScanProjectId(id);
        apiScanRepository.deleteByProjectId(id);
        projectRepository.delete(project);
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
