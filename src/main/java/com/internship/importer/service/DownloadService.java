package com.internship.importer.service;

import java.io.IOException;
import java.nio.file.Path;


public interface DownloadService {
    void downloadFile(String urlString, Path targetFolder) throws IOException;
}
