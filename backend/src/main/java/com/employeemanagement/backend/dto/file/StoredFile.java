package com.employeemanagement.backend.dto.file;

public record StoredFile(byte[] content, String contentType) {
}
