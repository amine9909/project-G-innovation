package com.employeemanagement.backend.controller;

import com.employeemanagement.backend.dto.employee.CreateEmployeeRequest;
import com.employeemanagement.backend.dto.employee.EmployeeImportResponse;
import com.employeemanagement.backend.dto.employee.EmployeeResponse;
import com.employeemanagement.backend.dto.employee.UpdateEmployeeRequest;
import com.employeemanagement.backend.dto.file.StoredFile;
import com.employeemanagement.backend.service.ContractService;
import com.employeemanagement.backend.service.EmployeeImportExportService;
import com.employeemanagement.backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ContractService contractService;
    private final EmployeeImportExportService employeeImportExportService;

    public EmployeeController(EmployeeService employeeService, ContractService contractService,
                              EmployeeImportExportService employeeImportExportService) {
        this.employeeService = employeeService;
        this.contractService = contractService;
        this.employeeImportExportService = employeeImportExportService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_CREATE')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateTo,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(employeeService.getEmployees(
                search,
                department,
                jobTitle,
                hireDateFrom,
                hireDateTo,
                salaryMin,
                salaryMax,
                sortBy,
                sortDir,
                page,
                size
        ));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> exportEmployeesToExcel() {
        byte[] excel = employeeImportExportService.exportEmployeesToExcel();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.xlsx\"")
                .body(excel);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> exportEmployeesToPdf() {
        byte[] pdf = employeeImportExportService.exportEmployeesToPdf();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.pdf\"")
                .body(pdf);
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] template = employeeImportExportService.generateImportTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee-import-template.xlsx\"")
                .body(template);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_CREATE')")
    public ResponseEntity<EmployeeImportResponse> importEmployees(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(employeeImportExportService.importEmployees(file));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<EmployeeResponse> uploadPhoto(@PathVariable Long id,
                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(employeeService.uploadPhoto(id, file));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        StoredFile storedFile = employeeService.getPhoto(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.contentType()))
                .body(storedFile.content());
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<EmployeeResponse> deletePhoto(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deletePhoto(id));
    }

    @PutMapping(value = "/{id}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<EmployeeResponse> uploadCv(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(employeeService.uploadCv(id, file));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> getCv(@PathVariable Long id) {
        StoredFile storedFile = employeeService.getCv(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"employee-" + id + "-cv.pdf\"")
                .body(storedFile.content());
    }

    @GetMapping("/{id}/cv/download")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> downloadCv(@PathVariable Long id) {
        StoredFile storedFile = employeeService.getCv(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee-" + id + "-cv.pdf\"")
                .body(storedFile.content());
    }

    @DeleteMapping("/{id}/cv")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_UPDATE')")
    public ResponseEntity<EmployeeResponse> deleteCv(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deleteCv(id));
    }

    @GetMapping("/{id}/contract")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_READ')")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
        byte[] pdf = contractService.generateEmployeeContract(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee-" + id + "-contract.pdf\"")
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_DELETE')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
