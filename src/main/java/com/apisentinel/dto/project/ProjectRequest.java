package com.apisentinel.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
//Data Transfer Object
//ProjectRequest for User-to-Server communication.
public class ProjectRequest {

    
    @NotBlank(message = "Project name is required")
    @Size(max = 120, message = "Project name must be at most 120 characters")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
