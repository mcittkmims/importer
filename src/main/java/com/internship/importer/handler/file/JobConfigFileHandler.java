package com.internship.importer.handler.file;

import com.internship.importer.exception.InvalidJobConfigFileException;
import com.internship.importer.exception.JobConfigException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobConfigFileHandler {

    @Value("${importer.job.config.folder}")
    private String configFolderPath;

    private static final Pattern JOB_CONFIG_PATTERN = Pattern.compile("^([\\w-]+)\\.config\\.json$");

    public Map<String, String> loadJobConfigs() {
        File[] configFiles = listJobConfigFiles();
        Map<String, String> jobConfigs = new HashMap<>();

        for (File file : configFiles) {
            String jobName = extractJobNameFromFilename(file.getName());
            String content = readFileContent(file.toPath());
            jobConfigs.put(jobName, content);
        }

        return jobConfigs;
    }

    private File[] listJobConfigFiles() {
        Path configPath = Paths.get(configFolderPath);
        File configDir = configPath.toFile();

        if (!configDir.exists() || !configDir.isDirectory()) {
            throw new JobConfigException("Config folder does not exist or is not a directory: " + configDir.getAbsolutePath());
        }

        File[] files = configDir.listFiles(File::isFile);
        if (files == null) {
            throw new JobConfigException("Failed to list files in config folder: " + configDir.getAbsolutePath());
        }

        return files;
    }

    private String extractJobNameFromFilename(String fileName) {
        Matcher matcher = JOB_CONFIG_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new InvalidJobConfigFileException("Invalid job config filename: " + fileName +
                    ". Expected pattern: jobName.config.json");
        }
        return matcher.group(1);
    }

    private String readFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new JobConfigException("Failed to read job config file: " + path, e);
        }
    }
}
