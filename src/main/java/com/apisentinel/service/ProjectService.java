package com.apisentinel.service;

import com.apisentinel.dto.project.ProjectRequest;
import com.apisentinel.dto.project.ProjectResponse;
import com.apisentinel.entity.Project;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//Used for business logic related to Project entity. It handles the processing of project data, including creating, retrieving, updating, and deleting projects. The service interacts with the ProjectRepository to perform database operations and converts between ProjectRequest and ProjectResponse DTOs for communication with the controller layer.
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project(request.getName().trim(), normalizeDescription(request.getDescription()));
        Project savedProject = projectRepository.save(project);
        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.getName().trim());
        project.setDescription(normalizeDescription(request.getDescription()));
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findProject(id);
        projectRepository.delete(project);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " was not found"));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
