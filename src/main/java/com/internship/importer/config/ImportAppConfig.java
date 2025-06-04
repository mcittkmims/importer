package com.internship.importer.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:importapp.properties")
public class ImportAppConfig {

    @Value("${importapp.download.url}")
    private String downloadUrl;

    @Value("${importapp.http.method:GET}")
    private String httpMethod;

    @Value("${importapp.batch.size:100}")
    private Integer batchSize;

    @Value("${importapp.download.location:downloads}")
    private String downloadLocation;

    @Value("${importapp.scheduler.cron}")
    private String schedulerCron;

    @Value("${importapp.company.corporate.number:corporate_number}")
    private String companyCorporateNumberField;
    @Value("${importapp.company.name:name}")
    private String companyNameField;
    @Value("${importapp.company.status:status}")
    private String companyStatusField;
    @Value("${importapp.company.update.date:update_date}")
    private String companyUpdateDateField;
    @Value("${importapp.company.location:location}")
    private String companyLocationField;
    @Value("${importapp.company.postal.code:postal_code}")
    private String companyPostalCodeField;
    @Value("${importapp.company.representative.name:representative_name}")
    private String companyRepresentativeNameField;
    @Value("${importapp.company.representative.title:representative_position}")
    private String companyRepresentativeTitleField;
    @Value("${importapp.company.employee.count:employee_number}")
    private String companyEmployeeCountField;
    @Value("${importapp.company.establishment.date:date_of_establishment}")
    private String companyEstablishmentDateField;
    @Value("${importapp.company.industries:business_items}")
    private String companyIndustriesField;


}
