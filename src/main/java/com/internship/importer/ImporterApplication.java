package com.internship.importer;

import com.internship.importer.service.ZipDownloadService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class ImporterApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(ImporterApplication.class, args);
    }


}
