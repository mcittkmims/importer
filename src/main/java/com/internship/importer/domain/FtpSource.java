package com.internship.importer.domain;

import lombok.Data;

@Data
public class FtpSource {
    private String url;
    private String host;
    private int port;
    private String username;
    private String password;
    private String path;
}
