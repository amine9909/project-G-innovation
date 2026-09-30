package com.employeemanagement.backend.service;

import com.employeemanagement.backend.dto.file.StoredFile;
import com.employeemanagement.backend.dto.employee.CreateEmployeeRequest;
import com.employeemanagement.backend.dto.employee.EmployeeResponse;
import com.employeemanagement.backend.dto.employee.UpdateEmployeeRequest;
import com.employeemanagement.backend.entity.Employee;
import com.employeemanagement.backend.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmployeeService {

    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_CV_SIZE_BYTES = 10 * 1024 * 1024;

    private final EmployeeRepository employeeRepository;
    private final MinioStorageService minioStorageService;
    private final ActivityNotificationService activityNotificationService;

    public EmployeeService(EmployeeRepository employeeRepository, MinioStorageService minioStorageService,
                           ActivityNotificationService activityNotificationService) {
        this.employeeRepository = employeeRepository;
        this.minioStorageService = minioStorageService;
        this.activityNotificationService = activityNotificationService;
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setJobTitle(request.getJobTitle());
        employee.setDepartment(request.getDepartment());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());
        employee.setPhotoObjectName(null);
        employee.setCvObjectName(null);

        Employee savedEmployee = employeeRepository.save(employee);
        activityNotificationService.recordEmployeeActivity(ActivityNotificationService.EMPLOYEE_CREATED, savedEmployee);
        return toEmployeeResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployees(String search, String department, String jobTitle,
                                               LocalDate hireDateFrom, LocalDate hireDateTo,
                                               BigDecimal salaryMin, BigDecimal salaryMax,
                                               String sortBy, String sortDir, int page, int size) {
        validateFilterRanges(hireDateFrom, hireDateTo, salaryMin, salaryMax);

        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy, sortDir));
        Specification<Employee> specification = buildEmployeeSpecification(
                search,
                department,
                jobTitle,
                hireDateFrom,
                hireDateTo,
                salaryMin,
                salaryMax
        );

        return employeeRepository.findAll(specification, pageable).map(this::toEmployeeResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        return toEmployeeResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        employeeRepository.findByEmail(request.getEmail())
                .filter(existingEmployee -> !existingEmployee.getId().equals(id))
                .ifPresent(existingEmployee -> {
                    throw new IllegalArgumentException("Email is already in use");
                });

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setJobTitle(request.getJobTitle());
        employee.setDepartment(request.getDepartment());
        employee.setHireDate(request.getHireDate());
        employee.setSalary(request.getSalary());

        Employee savedEmployee = employeeRepository.save(employee);
        activityNotificationService.recordEmployeeActivity(ActivityNotificationService.EMPLOYEE_UPDATED, savedEmployee);
        return toEmployeeResponse(savedEmployee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        String photoObjectName = employee.getPhotoObjectName();
        String cvObjectName = employee.getCvObjectName();

        if (photoObjectName != null && !photoObjectName.isBlank()) {
            minioStorageService.delete(photoObjectName);
        }

        if (cvObjectName != null && !cvObjectName.isBlank()) {
            minioStorageService.delete(cvObjectName);
        }

        activityNotificationService.recordEmployeeActivity(ActivityNotificationService.EMPLOYEE_DELETED, employee);
        employeeRepository.delete(employee);
    }

    @Transactional
    public EmployeeResponse uploadPhoto(Long id, MultipartFile file) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        validatePhoto(file);

        String objectName = generatePhotoObjectName(id, file.getContentType());
        minioStorageService.upload(objectName, file);

        String oldPhotoObjectName = employee.getPhotoObjectName();
        boolean replacingPhoto = oldPhotoObjectName != null && !oldPhotoObjectName.isBlank();
        employee.setPhotoObjectName(objectName);
        Employee savedEmployee = employeeRepository.save(employee);

        if (replacingPhoto) {
            minioStorageService.delete(oldPhotoObjectName);
        }

        activityNotificationService.recordEmployeeActivity(
                replacingPhoto ? ActivityNotificationService.PHOTO_REPLACED : ActivityNotificationService.PHOTO_UPLOADED,
                savedEmployee
        );
        return toEmployeeResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public StoredFile getPhoto(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        if (employee.getPhotoObjectName() == null || employee.getPhotoObjectName().isBlank()) {
            throw new EntityNotFoundException("Employee photo not found");
        }

        return minioStorageService.get(employee.getPhotoObjectName());
    }

    @Transactional
    public EmployeeResponse deletePhoto(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        if (employee.getPhotoObjectName() == null || employee.getPhotoObjectName().isBlank()) {
            return toEmployeeResponse(employee);
        }

        String oldPhotoObjectName = employee.getPhotoObjectName();
        employee.setPhotoObjectName(null);
        Employee savedEmployee = employeeRepository.save(employee);
        minioStorageService.delete(oldPhotoObjectName);

        activityNotificationService.recordEmployeeActivity(ActivityNotificationService.PHOTO_DELETED, savedEmployee);
        return toEmployeeResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse uploadCv(Long id, MultipartFile file) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        validateCv(file);

        String objectName = generateCvObjectName(id);
        minioStorageService.upload(objectName, file);

        String oldCvObjectName = employee.getCvObjectName();
        boolean replacingCv = oldCvObjectName != null && !oldCvObjectName.isBlank();
        employee.setCvObjectName(objectName);
        Employee savedEmployee = employeeRepository.save(employee);

        if (replacingCv) {
            minioStorageService.delete(oldCvObjectName);
        }

        activityNotificationService.recordEmployeeActivity(
                replacingCv ? ActivityNotificationService.CV_REPLACED : ActivityNotificationService.CV_UPLOADED,
                savedEmployee
        );
        return toEmployeeResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public StoredFile getCv(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        if (employee.getCvObjectName() == null || employee.getCvObjectName().isBlank()) {
            throw new EntityNotFoundException("Employee CV not found");
        }

        return minioStorageService.get(employee.getCvObjectName());
    }

    @Transactional
    public EmployeeResponse deleteCv(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        if (employee.getCvObjectName() == null || employee.getCvObjectName().isBlank()) {
            return toEmployeeResponse(employee);
        }

        String oldCvObjectName = employee.getCvObjectName();
        employee.setCvObjectName(null);
        Employee savedEmployee = employeeRepository.save(employee);
        minioStorageService.delete(oldCvObjectName);

        activityNotificationService.recordEmployeeActivity(ActivityNotificationService.CV_DELETED, savedEmployee);
        return toEmployeeResponse(savedEmployee);
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file must not be empty");
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo file size must not exceed 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Photo content type is required");
        }

        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Photo file must be an image");
        }

        if (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png")
                && !contentType.equals("image/webp")) {
            throw new IllegalArgumentException("Photo content type must be image/jpeg, image/png, or image/webp");
        }
    }

    private String generatePhotoObjectName(Long employeeId, String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported photo content type");
        };

        return "employees/" + employeeId + "/photo/" + UUID.randomUUID() + "." + extension;
    }

    private void validateCv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CV file must not be empty");
        }

        if (file.getSize() > MAX_CV_SIZE_BYTES) {
            throw new IllegalArgumentException("CV file size must not exceed 10 MB");
        }

        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            throw new IllegalArgumentException("CV content type must be application/pdf");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(5);
            if (signature.length != 5
                    || signature[0] != '%'
                    || signature[1] != 'P'
                    || signature[2] != 'D'
                    || signature[3] != 'F'
                    || signature[4] != '-') {
                throw new IllegalArgumentException("CV file must be a valid PDF");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to validate CV file", exception);
        }
    }

    private String generateCvObjectName(Long employeeId) {
        return "employees/" + employeeId + "/cv/" + UUID.randomUUID() + ".pdf";
    }

    private void validateFilterRanges(LocalDate hireDateFrom, LocalDate hireDateTo,
                                      BigDecimal salaryMin, BigDecimal salaryMax) {
        if (hireDateFrom != null && hireDateTo != null && hireDateFrom.isAfter(hireDateTo)) {
            throw new IllegalArgumentException("Hire date from must be before or equal to hire date to");
        }

        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new IllegalArgumentException("Salary min must be less than or equal to salary max");
        }
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted();
        }

        Sort.Direction direction = resolveSortDirection(sortDir);

        return switch (sortBy.trim()) {
            case "name" -> Sort.by(direction, "firstName", "lastName");
            case "hireDate" -> Sort.by(direction, "hireDate");
            case "salary" -> Sort.by(direction, "salary");
            default -> throw new IllegalArgumentException("Invalid employee sort field");
        };
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return Sort.Direction.ASC;
        }

        return switch (sortDir.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Invalid employee sort direction");
        };
    }

    private Specification<Employee> buildEmployeeSpecification(String search, String department, String jobTitle,
                                                               LocalDate hireDateFrom, LocalDate hireDateTo,
                                                               BigDecimal salaryMin, BigDecimal salaryMax) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            String normalizedSearch = normalizeFilterValue(search);
            if (normalizedSearch != null) {
                String likeSearch = "%" + normalizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likeSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likeSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likeSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), likeSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), likeSearch)
                ));
            }

            String normalizedDepartment = normalizeFilterValue(department);
            if (normalizedDepartment != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("department")),
                        "%" + normalizedDepartment + "%"
                ));
            }

            String normalizedJobTitle = normalizeFilterValue(jobTitle);
            if (normalizedJobTitle != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("jobTitle")),
                        "%" + normalizedJobTitle + "%"
                ));
            }

            if (hireDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("hireDate"), hireDateFrom));
            }

            if (hireDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("hireDate"), hireDateTo));
            }

            if (salaryMin != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salary"), salaryMin));
            }

            if (salaryMax != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salary"), salaryMax));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeFilterValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        boolean hasPhoto = employee.getPhotoObjectName() != null && !employee.getPhotoObjectName().isBlank();
        boolean hasCv = employee.getCvObjectName() != null && !employee.getCvObjectName().isBlank();

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhoneNumber(),
                employee.getJobTitle(),
                employee.getDepartment(),
                employee.getHireDate(),
                employee.getSalary(),
                hasPhoto,
                hasCv
        );
    }
}
