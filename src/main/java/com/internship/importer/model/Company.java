package com.internship.importer.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Company {
    private Long id;
    private String corporateNumber;
    private String name;
    private String status;
    private LocalDateTime updateDate;
    private String location;
    private String postalCode;
    private String representativeName;
    private String representativeTitle;
    private Integer employeeCount;
    private LocalDate establishmentDate;
}
