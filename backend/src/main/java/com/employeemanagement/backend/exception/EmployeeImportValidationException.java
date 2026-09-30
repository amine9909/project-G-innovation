package com.employeemanagement.backend.exception;

import java.util.List;

public class EmployeeImportValidationException extends RuntimeException {

    private final List<String> errors;

    public EmployeeImportValidationException(List<String> errors) {
        super("Employee import validation failed.");
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public int getErrorCount() {
        return errors.size();
    }

    public List<String> getErrors() {
        return errors;
    }
}
