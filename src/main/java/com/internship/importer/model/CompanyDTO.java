package com.internship.importer.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class CompanyDTO {
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

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> industries;

    public Company toCompany() {
        Company company = new Company();
        company.setCorporateNumber(corporateNumber);
        company.setName(name);
        company.setStatus(status);
        company.setUpdateDate(updateDate);
        company.setLocation(location);
        company.setPostalCode(postalCode);
        company.setRepresentativeName(representativeName);
        company.setRepresentativeTitle(representativeTitle);
        return company;
    }
    public List<Industry> toIndustries() {
        if (this.industries == null) {
            return List.of();
        }
        return this.industries.stream().map(s -> {
            Industry industry = new Industry();
            industry.setCode(s);
            return industry;
        }).collect(Collectors.toList());
    }

    public List<CompanyIndustry> toCompanyIndustries() {
        if (this.industries == null) {
            return List.of();
        }
        return this.toIndustries().stream().map(industry -> {
            CompanyIndustry companyIndustry = new CompanyIndustry();
            companyIndustry.setIndustry(industry);
            companyIndustry.setCompany(this.toCompany());
            return companyIndustry;
        }).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "CompanyDTO{" +
                "corporateNumber='" + corporateNumber + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", updateDate=" + updateDate +
                ", location='" + location + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", representativeName='" + representativeName + '\'' +
                ", representativeTitle='" + representativeTitle + '\'' +
                ", employeeCount=" + employeeCount +
                ", establishmentDate=" + establishmentDate +
                ", industries=" + industries +
                '}';
    }
}


