package com.internship.importer.core.job;

import com.internship.importer.infrastructure.persistence.JobConfigurationLoader;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JobLoader {

    private JobConfigurationLoader jobConfigurationLoader;
    private JobFactory jobFactory;
    @Getter
    private List<Job> jobs = new ArrayList<>();

    public JobLoader(JobConfigurationLoader jobConfigurationLoader, JobFactory jobFactory) {
        this.jobConfigurationLoader = jobConfigurationLoader;
        this.jobFactory = jobFactory;
    }

    @PostConstruct
    public void extractJobs() {
        Map<String, String> configs = jobConfigurationLoader.loadJobConfigs();
        for (String key : configs.keySet()) {
            Job job = jobFactory.createJob(key, configs.get(key));
            jobs.add(job);
        }
    }
}
