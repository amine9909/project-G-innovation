package com.employeemanagement.backend.dto.employee;

import java.util.List;

public record EmployeeImportErrorResponse(
        String message,
        int errorCount,
        List<String> errors
) {

    public EmployeeImportErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
