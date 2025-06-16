package com.internship.importer.exception;

import java.io.IOException;

public class StatusFileException extends RuntimeException {
    public StatusFileException(String e, Throwable err) {
        super(e, err);
    }

    public StatusFileException(String e) {
        super(e);
    }
}
