package com.internship.importer.service;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.mapper.CompanyMapper;
import com.internship.importer.model.Company;
import com.internship.importer.model.CompanyDTO;
import com.internship.importer.model.CompanyIndustry;
import com.internship.importer.model.Industry;
import com.internship.importer.transformer.CompanyDataTransformer;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@AllArgsConstructor
public class CompanyService {
    private DownloadService downloadService;
    private CompanyDataTransformer transformer;
    private CompanyMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private ImportAppConfig config;

    public void importCompanyData(String url, String path) throws IOException {
        Path targetFolder = Paths.get(path);
        downloadService.downloadFile(url,targetFolder);
        List<CompanyDTO> companies = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetFolder)) {
            for (Path filePath : stream) {
                if (Files.isRegularFile(filePath)) {
                    try (InputStream inputStream = Files.newInputStream(filePath)) {
                        log.info("Processing file {} @ {}", filePath, Instant.now());
                        this.transformer.processLargeFile(inputStream, this.getConsumer(companies));
                    }
                    Files.delete(filePath);

                }
            }
        }
        this.insertIntoDatabase(companies);
    }

    private void insertIntoDatabase(List<CompanyDTO> companyDTOList) {
        List<Company> companies = new ArrayList<>();
        List<Industry> industries = new ArrayList<>();
        List<CompanyIndustry> companyIndustries = new ArrayList<>();

        for (CompanyDTO companyDTO : companyDTOList) {
            if (companyDTO.getName() == null || companyDTO.getCorporateNumber() == null) {
                log.warn("Skipped 1 object because of incomplete data: {}", companyDTO);
                continue;
            }
            companies.add(companyDTO.toCompany());
            industries.addAll(companyDTO.toIndustries());
            companyIndustries.addAll(companyDTO.toCompanyIndustries());
        }
        if (companies.isEmpty() || industries.isEmpty() || companyIndustries.isEmpty()) {
            companyDTOList.clear();
            return;
        }
        mapper.insertCompanies(companies);
        mapper.insertIndustries(industries);
        mapper.insertCompanyIndustries(companyIndustries);
        companyDTOList.clear();

    }

    private Consumer<CompanyDTO> getConsumer(List<CompanyDTO> companies){
        return company ->
        {
            companies.add(company);
            if (companies.size() == config.getBatchSize()) {
                this.insertIntoDatabase(companies);
            }
        };
    }

}
