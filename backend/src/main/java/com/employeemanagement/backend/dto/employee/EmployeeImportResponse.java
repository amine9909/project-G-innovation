package com.employeemanagement.backend.dto.employee;

public class EmployeeImportResponse {

    private final String message;
    private final int importedCount;

    public EmployeeImportResponse(String message, int importedCount) {
        this.message = message;
        this.importedCount = importedCount;
    }

    public String getMessage() {
        return message;
    }

    public int getImportedCount() {
        return importedCount;
    }
}
