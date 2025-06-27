package com.internship.importer.repository;

import com.internship.importer.domain.JsonDataRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RepositoryHelper {

    public void markRowsWithTransaction(StagingRepository repository, List<Long> ids) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(repository.getDataSource());
        TransactionTemplate txTemplate = new TransactionTemplate(txManager);

        txTemplate.executeWithoutResult(status -> {
            repository.markRowsByIds(ids);
        });
    }

    public void createTable(StagingTableService service) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(service.getDataSource());
        TransactionTemplate txTemplate = new TransactionTemplate(txManager);

        txTemplate.executeWithoutResult(status -> {
            service.createStagingTable();
        });
    }


}
