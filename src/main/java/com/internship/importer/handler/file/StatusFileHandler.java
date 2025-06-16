package com.internship.importer.handler.file;

import com.internship.importer.exception.FolderCreationException;
import com.internship.importer.exception.StatusFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

@Component
public class StatusFileHandler {

    @Value("${importer.job.status.logging.path}")
    private String path;

    public void createSuccessStatusFile(String jobName, String task) {
        Path dirPath = Paths.get(path, task);
        File dir = dirPath.toFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new FolderCreationException("Failed to create directory: " + dir.getAbsolutePath());
            }
        }

        File successFile = new File(dir, jobName + ".success");

        try {
            if (!successFile.createNewFile()) {
                throw new StatusFileException("Success file already exists at " + successFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new StatusFileException("Failed to create success file " + successFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    public void deleteStatusFileIfExists(String jobName, String task) {
        Path dirPath = Paths.get(path, task);
        File successFile = new File(dirPath.toFile(), jobName + ".success");

        if (successFile.exists()) {
            boolean deleted = successFile.delete();
            if (!deleted) {
                throw new StatusFileException("Failed to delete success file: " + successFile.getAbsolutePath());
            }
        }
    }

    public boolean statusFileExists(String jobName, String task) {
        Path dirPath = Paths.get(path, task);
        File successFile = new File(dirPath.toFile(), jobName + ".success");
        return successFile.exists();
    }

    public Instant getStatusFileCreationTime(String jobName, String task) {
        Path filePath = Paths.get(path, task, jobName + ".success");
        try {
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            return attr.creationTime().toInstant();
        } catch (IOException e) {
            throw new StatusFileException("Could not get creation time for file: " + filePath, e);
        }
    }





}
