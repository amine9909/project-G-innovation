package com.employeemanagement.backend.dto.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeResponse {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final String jobTitle;
    private final String department;
    private final LocalDate hireDate;
    private final BigDecimal salary;
    private final boolean hasPhoto;
    private final boolean hasCv;

    public EmployeeResponse(Long id, String firstName, String lastName, String email, String phoneNumber,
                            String jobTitle, String department, LocalDate hireDate, BigDecimal salary,
                            boolean hasPhoto, boolean hasCv) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.jobTitle = jobTitle;
        this.department = department;
        this.hireDate = hireDate;
        this.salary = salary;
        this.hasPhoto = hasPhoto;
        this.hasCv = hasCv;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public boolean isHasCv() {
        return hasCv;
    }
}
