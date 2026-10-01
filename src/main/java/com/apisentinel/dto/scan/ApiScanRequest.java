package com.apisentinel.dto.scan;

import jakarta.validation.constraints.Size;

public class ApiScanRequest {

    @Size(max = 255, message = "OpenAPI file name must be at most 255 characters")
    private String openApiFileName;

    public String getOpenApiFileName() {
        return openApiFileName;
    }

    public void setOpenApiFileName(String openApiFileName) {
        this.openApiFileName = openApiFileName;
    }
}
