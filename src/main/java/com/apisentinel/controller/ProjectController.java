package com.apisentinel.controller;

import com.apisentinel.dto.project.ProjectRequest;
import com.apisentinel.dto.project.ProjectResponse;
import com.apisentinel.security.CurrentUser;
import com.apisentinel.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(CurrentUser.idOf(jwt), request);
    }

    @GetMapping
    public List<ProjectResponse> getProjects(@AuthenticationPrincipal Jwt jwt) {
        return projectService.getProjects(CurrentUser.idOf(jwt));
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return projectService.getProject(CurrentUser.idOf(jwt), id);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.updateProject(CurrentUser.idOf(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        projectService.deleteProject(CurrentUser.idOf(jwt), id);
    }
}
