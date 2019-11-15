package org.springframework.data.mongodb.datatables;

import com.mongodb.BasicDBObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

final class DataTablesRefCriteria {
    private final Query query = new Query();
    private Aggregation aggregation;

    DataTablesRefCriteria(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria) {

        List<AggregationOperation> aggregationOperations = addReferenceResolver(input);

        AggregationOperation globalMatching = addGlobalCriteria(input);

        if (globalMatching != null) {
            aggregationOperations.add(globalMatching);
        }

        // NOT YET ADAPTED [START]
        input.getColumns().forEach(this::addColumnCriteria);
        addSort(input);
        if (additionalCriteria != null) query.addCriteria(additionalCriteria);
        if (preFilteringCriteria != null) query.addCriteria(preFilteringCriteria);
        // NOT YET ADAPTED [END]

        aggregation = Aggregation.newAggregation(aggregationOperations);
    }

    private List<AggregationOperation> addReferenceResolver(DataTablesInput input) {

        List<AggregationOperation> aggregations = new ArrayList<>();

        List<String> columnStrings = input.getColumns().stream()
                .map(column -> column.getData())
                .collect(toList());

        for (DataTablesInput.Column c: input.getColumns()) {
            if (c.isReference()) {

                String[] columnStringsArr = columnStrings.toArray(new String[0]);

                // Convert reference field array of key-value objects
                ProjectionOperation projectDbRefArr = Aggregation
                        .project(columnStringsArr)
                        .and(ObjectOperators.ObjectToArray.valueOfToArray(c.getData()))
                        .as(c.getData() + "_fk_arr");

                // Extract object with Id from array
                ProjectionOperation projectDbRefObject = Aggregation
                        .project(columnStringsArr)
                        .and( c.getData() + "_fk_arr").arrayElementAt(1)
                        .as(c.getData() + "_fk_obj");

                // Get value field from key-value object
                ProjectionOperation projectPidField = Aggregation
                        .project(columnStringsArr)
                        .and(c.getData() + "_fk_obj.v").as(c.getData() + "_id");

                // Lookup object with id in reference collection and save it in document
                LookupOperation lookupOperation = Aggregation
                        .lookup(c.getReferenceCollection(), c.getData() + "_id", "_id", c.getData() + "_resolved");

                // Make sure resolved object stays in future projections
                columnStrings.add(c.getData() + "_resolved");

                aggregations.add(projectDbRefArr);
                aggregations.add(projectDbRefObject);
                aggregations.add(projectPidField);
                aggregations.add(lookupOperation);
            }
        }

        return aggregations;
    }

    Query toQuery() {
        return query;
    }

    private AggregationOperation addGlobalCriteria(DataTablesInput input) {
        if (!hasText(input.getSearch().getValue())) return null;

        Criteria[] criteriaArray = input.getColumns().stream()
                .filter(DataTablesInput.Column::isSearchable)
                .map(column -> createCriteriaRefSupport(column, input.getSearch()))
                .flatMap(criteraList -> criteraList.stream())
                .toArray(Criteria[]::new);

        if (criteriaArray.length == 1) {
            return Aggregation.match(criteriaArray[0]);
        } else if (criteriaArray.length >= 2) {
            return Aggregation.match(new Criteria().orOperator(criteriaArray));
        } else {
            return null;
        }
    }

    private void addColumnCriteria(DataTablesInput.Column column) {
        if (column.isSearchable() && hasText(column.getSearch().getValue())) {
            query.addCriteria(createColumnCriteria(column));
        }
    }

    private Criteria createColumnCriteria(DataTablesInput.Column column) {
        String searchValue = column.getSearch().getValue();
        if ("true".equalsIgnoreCase(searchValue) || "false".equalsIgnoreCase(searchValue)) {
            return where(column.getData()).is(Boolean.valueOf(searchValue));
        } else {
            return createCriteria(column, column.getSearch());
        }
    }

    private Criteria createCriteria(DataTablesInput.Column column, DataTablesInput.Search search) {
        String searchValue = search.getValue();
        if (search.isRegex()) {
            return where(column.getData()).regex(searchValue);
        } else {
            return where(column.getData()).regex(searchValue.trim(), "i");
        }
    }

    private List<Criteria> createCriteriaRefSupport(DataTablesInput.Column column, DataTablesInput.Search search) {

        String searchValue = search.getValue();

        if (column.isReference()) {
            return column.getReferenceColumns().stream()
                    .map(data -> search.isRegex() ?
                            where(column.getData() + "_resolved." + data).regex(searchValue) : where(column.getData() + "_resolved." + data).regex(searchValue.trim(), "i"))
                    .collect(toList());


        } else {
            List<Criteria> criteria = new ArrayList<>();
            if (search.isRegex()) {
                criteria.add(where(column.getData()).regex(searchValue));
            } else {
                criteria.add(where(column.getData()).regex(searchValue.trim(), "i"));
            }

            return criteria;
        }
    }

    private void addSort(DataTablesInput input) {
        query.skip(input.getStart());
        query.limit(input.getLength());

        if (isEmpty(input.getOrder())) return;

        List<Sort.Order> orders = input.getOrder().stream()
                .filter(order -> isOrderable(input, order))
                .map(order -> toOrder(input, order)).collect(toList());
        query.with(by(orders));
    }

    private boolean isOrderable(DataTablesInput input, DataTablesInput.Order order) {
        boolean isWithinBounds = order.getColumn() < input.getColumns().size();
        return isWithinBounds && input.getColumns().get(order.getColumn()).isOrderable();
    }

    private Sort.Order toOrder(DataTablesInput input, DataTablesInput.Order order) {
        return new Sort.Order(
                order.getDir() == DataTablesInput.Order.Direction.asc ? Sort.Direction.ASC : Sort.Direction.DESC,
                input.getColumns().get(order.getColumn()).getData()
        );
    }

    public Aggregation toAggregation() {
        return aggregation;
    }
}