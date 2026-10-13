package com.apisentinel.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Describes the API in the generated OpenAPI document and adds the "Authorize" button to Swagger UI.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Sentinel",
                version = "1.0",
                description = "Static security analysis for OpenAPI specifications. "
                        + "Register and log in under /api/auth, then use Authorize with the access token."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
