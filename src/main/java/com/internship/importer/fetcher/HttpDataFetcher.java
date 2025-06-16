package com.internship.importer.fetcher;


import com.internship.importer.exception.UrlConnectionException;
import com.internship.importer.exception.ZipExtractionException;
import com.internship.importer.model.HttpSource;
import lombok.AllArgsConstructor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@AllArgsConstructor
public class HttpDataFetcher implements DataFetcher{

    private HttpSource httpSource;

    @Override
    public InputStream fetchData() {
        try {
            HttpURLConnection connection = createConnection(httpSource.getUrl(), httpSource.getMethod());
            return connection.getInputStream();
        } catch (IOException e) {
            throw new UrlConnectionException("Failed to fetch data from URL: " + httpSource.getUrl(), e);
        }
    }



    private HttpURLConnection createConnection(String urlString, String httpMethod){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod(httpMethod);
            connection.setDoOutput(true);
            connection.connect();
            return connection;
        } catch (IOException e) {
            throw new UrlConnectionException("Failed to connect to URL: " + urlString, e);
        }
    }


}
