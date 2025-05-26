package com.internship.importer.model;

import lombok.Setter;

@Setter
public class CompanyIndustry {
    Company company;
    Industry industry;

    public String getCompanyCode() {
        return company.getCorporateNumber();
    }

    public String getIndustryCode() {
        return industry.getCode();
    }
}
