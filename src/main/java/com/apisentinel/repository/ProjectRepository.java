package com.apisentinel.repository;

import com.apisentinel.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

//Used for communication with the database for Project entity.
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
