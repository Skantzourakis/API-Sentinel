package com.apisentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//Used for bootstrapping the Spring Boot application. It contains the main method that serves as the entry point for the application. The @SpringBootApplication annotation indicates that this is a Spring Boot application and enables auto-configuration, component scanning, and other features.
@SpringBootApplication
public class ApiSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSentinelApplication.class, args);
    }
}
