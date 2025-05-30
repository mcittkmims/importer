package com.internship.importer.tasks;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.service.CompanyService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@AllArgsConstructor
public class CompanyDataImportTask implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CompanyDataImportTask.class);
    private CompanyService service;
    private ImportAppConfig config;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(this::insertTask, config.getSchedulerCron());
    }


    public void insertTask(){
        log.info("Started importing company data @ {}",Instant.now());
        try {
            this.service.importCompanyData(config.getDownloadUrl(), config.getDownloadLocation());
        } catch (IOException e) {
            log.error("Import failed", e);
        }
    }
}
