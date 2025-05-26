package com.internship.importer.transformer;

import com.internship.importer.model.Company;
import com.internship.importer.model.CompanyDTO;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface CompanyDataTransformer {
    void processLargeFile(InputStream inputStream, Consumer<CompanyDTO> consumer) throws IOException;

}
