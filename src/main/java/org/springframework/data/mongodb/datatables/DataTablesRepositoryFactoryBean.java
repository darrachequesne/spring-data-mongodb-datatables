package org.springframework.data.mongodb.datatables;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.io.Serializable;

public final class DataTablesRepositoryFactoryBean<R extends MongoRepository<T, ID>, T, ID extends Serializable>
        extends MongoRepositoryFactoryBean<R, T, ID> {

    public DataTablesRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(MongoOperations operations) {
        return new DataTablesRepositoryFactory(operations);
    }

    private static class DataTablesRepositoryFactory extends MongoRepositoryFactory {

        /**
         * Creates a new {@link MongoRepositoryFactory} with the given {@link MongoOperations}.
         *
         * @param mongoOperations must not be {@literal null}.
         */
        DataTablesRepositoryFactory(MongoOperations mongoOperations) {
            super(mongoOperations);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            Class<?> repositoryInterface = metadata.getRepositoryInterface();
            if (DataTablesRepository.class.isAssignableFrom(repositoryInterface)) {
                return DataTablesRepositoryImpl.class;
            } else {
                return super.getRepositoryBaseClass(metadata);
            }
        }
    }
}
