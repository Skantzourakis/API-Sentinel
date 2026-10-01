package com.apisentinel.exception;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final List<String> details;

    public ApiErrorResponse(int status, String error, List<String> details) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }
}
