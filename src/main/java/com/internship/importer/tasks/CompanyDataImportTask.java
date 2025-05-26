package com.internship.importer.tasks;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CompanyDataImportTask {

    private static final Logger log = LoggerFactory.getLogger(CompanyDataImportTask.class);
    private CompanyService service;
    private ImportAppConfig config;

    public CompanyDataImportTask(CompanyService service) {
        this.service = service;
    }

    @Scheduled(fixedRateString = "${cleaner.fixedRate.in.milliseconds:86400000}")
    public void insertTask() throws IOException {
        log.info("Started importing company data @ {}",Instant.now());
        this.service.importCompanyData(config.getDownloadUrl(), config.getDownloadLocation());
    }
}
