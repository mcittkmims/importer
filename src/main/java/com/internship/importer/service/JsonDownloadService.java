package com.internship.importer.service;

import java.io.InputStream;
import java.util.function.Consumer;


public interface JsonDownloadService {
    void downloadFile(String urlString, String httpMethod, Consumer<InputStream> consumer);
}
