package com.internship.importer.job;

import com.internship.importer.handler.file.JobConfigFileHandler;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JobLoader {

    private JobConfigFileHandler jobConfigFileHandler;
    private JobFactory jobFactory;
    @Getter
    private List<Job> jobs = new ArrayList<>();

    public JobLoader(JobConfigFileHandler jobConfigFileHandler, JobFactory jobFactory){
        this.jobConfigFileHandler = jobConfigFileHandler;
        this.jobFactory = jobFactory;
    }


    @PostConstruct
    public void extractJobs(){
        Map<String, String> configs = jobConfigFileHandler.loadJobConfigs();
        for (String key: configs.keySet()){
            Job job = jobFactory.createJob(key ,configs.get(key));
            jobs.add(job);
        }
    }
}
