package org.springframework.data.mongodb.datatables;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.function.Function;

@NoRepositoryBean
public interface DataTablesRepository<T, ID extends Serializable> extends MongoRepository<T, ID> {

    /**
     * Returns the filtered list for the given {@link DataTablesInput}.
     *
     * @param input the {@link DataTablesInput} mapped from the Ajax request
     * @return a {@link DataTablesOutput}
     */
    DataTablesOutput<T> findAll(DataTablesInput input);

    /**
     * Returns the filtered list for the given {@link DataTablesInput}.
     *
     * @param input              the {@link DataTablesInput} mapped from the Ajax request
     * @param additionalCriteria an additional {@link Criteria} to apply to the query (with
     *                           an "AND" clause)
     * @return a {@link DataTablesOutput}
     */
    DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria);

    /**
     * Returns the filtered list for the given {@link DataTablesInput}.
     *
     * @param input                the {@link DataTablesInput} mapped from the Ajax request
     * @param additionalCriteria   an additional {@link Criteria} to apply to the query (with an "AND" clause)
     * @param preFilteringCriteria a pre-filtering {@link Criteria} to apply to the query (with an "AND" clause)
     * @return a {@link DataTablesOutput}
     */
    DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria);

    /**
     * Returns the filtered list for the given {@link DataTablesInput}.
     *
     * @param input     the {@link DataTablesInput} mapped from the Ajax request
     * @param converter the {@link Function} to apply to the results of the query
     * @return a {@link DataTablesOutput}
     */
    <R> DataTablesOutput<R> findAll(DataTablesInput input, Function<T, R> converter);

    /**
     * Returns the filtered list for the given {@link DataTablesInput}.
     *
     * @param input                the {@link DataTablesInput} mapped from the Ajax request
     * @param additionalCriteria   an additional {@link Criteria} to apply to the query (with an "AND" clause)
     * @param preFilteringCriteria a pre-filtering {@link Criteria} to apply to the query (with an "AND" clause)
     * @param converter            the {@link Function} to apply to the results of the query
     * @return a {@link DataTablesOutput}
     */
    <R> DataTablesOutput<R> findAll(DataTablesInput input, Criteria additionalCriteria,
                                    Criteria preFilteringCriteria, Function<T, R> converter);

}
