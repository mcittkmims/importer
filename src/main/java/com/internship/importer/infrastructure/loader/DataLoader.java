package com.internship.importer.infrastructure.loader;

import java.io.InputStream;

public interface DataLoader {
    void loadData(InputStream inputStream, String tableName);
}
