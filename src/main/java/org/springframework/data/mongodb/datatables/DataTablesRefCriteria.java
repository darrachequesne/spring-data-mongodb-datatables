package org.springframework.data.mongodb.datatables;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

final class DataTablesRefCriteria {
    private Map<String, String> resolvedColumn = new HashMap<>();
    private Aggregation aggregation;
    private Aggregation filteredCountAggregation;

    DataTablesRefCriteria(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria) {

        List<AggregationOperation> aggregationOperations = new ArrayList<>();

        if (additionalCriteria != null) aggregationOperations.add(Aggregation.match(additionalCriteria));
        if (preFilteringCriteria != null) aggregationOperations.add(Aggregation.match(preFilteringCriteria));

        aggregationOperations.addAll(addReferenceResolver(input));

        AggregationOperation globalMatching = addGlobalCriteria(input);

        if (globalMatching != null) {
            aggregationOperations.add(globalMatching);
        }

        input.getColumns().forEach(column -> {
            MatchOperation columnCriteriaMatcher = addColumnCriteria(column);
            if (columnCriteriaMatcher != null) {
                aggregationOperations.add(columnCriteriaMatcher);
            }
        });

        List<AggregationOperation> filteredCountOperations = new ArrayList<>(aggregationOperations);
        filteredCountOperations.add(Aggregation.count().as("filtered_count"));

        filteredCountAggregation = Aggregation.newAggregation(filteredCountOperations);

        aggregationOperations.addAll(addSort(input));
        aggregation = Aggregation.newAggregation(aggregationOperations);
    }

    private List<AggregationOperation> addReferenceResolver(DataTablesInput input) {

        List<AggregationOperation> aggregations = new ArrayList<>();

        List<String> columnStrings = input.getColumns().stream()
                .map(column -> column.getData().contains(".") ? column.getData().substring(0, column.getData().indexOf(".")) : column.getData())
                .distinct()
                .collect(toList());

        for (DataTablesInput.Column c: input.getColumns()) {
            if (c.isReference() && (c.isSearchable() || c.isOrderable())) {

                String resolvedReferenceColumn = getResolvedRefColumn(c, columnStrings);

                resolvedColumn.put(c.getData(), resolvedReferenceColumn);

                String[] columnStringsArr = columnStrings.toArray(new String[0]);

                // Convert reference field array of key-value objects
                ProjectionOperation projectDbRefArr = Aggregation
                        .project(columnStringsArr)
                        .and(ObjectOperators.ObjectToArray.valueOfToArray(c.getData()))
                        .as(resolvedReferenceColumn + "_fk_arr");

                // Extract object with Id from array
                ProjectionOperation projectDbRefObject = Aggregation
                        .project(columnStringsArr)
                        .and( resolvedReferenceColumn + "_fk_arr").arrayElementAt(1)
                        .as(resolvedReferenceColumn + "_fk_obj");

                // Get value field from key-value object
                ProjectionOperation projectPidField = Aggregation
                        .project(columnStringsArr)
                        .and(resolvedReferenceColumn + "_fk_obj.v").as(resolvedReferenceColumn + "_id");

                // Lookup object with id in reference collection and save it in document
                LookupOperation lookupOperation = Aggregation
                        .lookup(c.getReferenceCollection(), resolvedReferenceColumn + "_id", "_id", resolvedReferenceColumn);

                // Make sure resolved object stays in future projections
                columnStrings.add(resolvedReferenceColumn);

                aggregations.add(projectDbRefArr);
                aggregations.add(projectDbRefObject);
                aggregations.add(projectPidField);
                aggregations.add(lookupOperation);
            }
        }

        return aggregations;
    }

    private AggregationOperation addGlobalCriteria(DataTablesInput input) {
        if (!hasText(input.getSearch().getValue())) return null;

        Criteria[] criteriaArray = input.getColumns().stream()
                .filter(DataTablesInput.Column::isSearchable)
                .map(column -> createCriteriaRefSupport(column, input.getSearch()))
                .flatMap(criteriaList -> criteriaList.stream())
                .toArray(Criteria[]::new);

        if (criteriaArray.length == 1) {
            return Aggregation.match(criteriaArray[0]);
        } else if (criteriaArray.length >= 2) {
            return Aggregation.match(new Criteria().orOperator(criteriaArray));
        } else {
            return null;
        }
    }

    private MatchOperation addColumnCriteria(DataTablesInput.Column column) {
        if (column.isSearchable() && hasText(column.getSearch().getValue())) {
            List<Criteria> criteria = createCriteriaRefSupport(column, column.getSearch());
            if (criteria.size() == 1) {
                return Aggregation.match(criteria.get(0));
            } else if (criteria.size() >= 2) {
                return Aggregation.match(new Criteria().orOperator(criteria.toArray(new Criteria[0])));
            }
        }

        return null;
    }

    private List<Criteria> createCriteriaRefSupport(DataTablesInput.Column column, DataTablesInput.Search search) {

        String searchValue = search.getValue();
        boolean isBooleanSearch = "true".equalsIgnoreCase(searchValue) || "false".equalsIgnoreCase(searchValue);


        if (column.isReference()) {

            if (isBooleanSearch) {
                boolean booleanSearchValue = Boolean.valueOf(searchValue);

                return column.getReferenceColumns().stream()
                        .map(data -> where(resolvedColumn.get(column.getData()) + "." + data).is(booleanSearchValue))
                        .collect(toList());
            } else {
                return column.getReferenceColumns().stream()
                        .map(data -> search.isRegex() ?
                                where(resolvedColumn.get(column.getData()) + "." + data).regex(searchValue) : where(resolvedColumn.get(column.getData()) + "." + data).regex(searchValue.trim(), "i"))
                        .collect(toList());
            }
        } else if (isBooleanSearch) {
            Criteria c = where(column.getData()).is(Boolean.valueOf(searchValue));
            List<Criteria> criteria = new ArrayList<>();
            criteria.add(c);
            return criteria;
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

    private List<AggregationOperation> addSort(DataTablesInput input) {
        List<AggregationOperation> operations = new ArrayList<>();

        if (!isEmpty(input.getOrder())) {

            List<Sort.Order> orders = input.getOrder().stream()
                    .filter(order -> isOrderable(input, order))
                    .map(order -> toOrder(input, order)).collect(toList());

            if (orders.size() != 0) {
                operations.add(Aggregation.sort(by(orders)));
            }

        }

        operations.add(Aggregation.skip(input.getStart()));

        if (input.getLength() >= 0) {
            operations.add(Aggregation.limit(input.getLength()));
        }

        return operations;
    }

    private boolean isOrderable(DataTablesInput input, DataTablesInput.Order order) {
        boolean isWithinBounds = order.getColumn() < input.getColumns().size();

        DataTablesInput.Column column = input.getColumns().get(order.getColumn());
        return isWithinBounds && column.isOrderable()
                && (!column.isReference() || (column.isReference() && !StringUtils.isEmpty(column.getReferenceOrderColumn())));
    }

    private Sort.Order toOrder(DataTablesInput input, DataTablesInput.Order order) {
        DataTablesInput.Column column = input.getColumns().get(order.getColumn());
        Sort.Direction sortDir = order.getDir() == DataTablesInput.Order.Direction.asc ? Sort.Direction.ASC : Sort.Direction.DESC;

        if (column.isReference()) {
            return new Sort.Order(sortDir, resolvedColumn.get(column.getData()) + "." + column.getReferenceOrderColumn());
        }


        return new Sort.Order(sortDir, column.getData());
    }

    private String getResolvedRefColumn(DataTablesInput.Column c, List<String> columnStrings) {

        String resolvedColumn = c.getData();
        boolean columnAlreadyExists;

        do {
            resolvedColumn += "_";
            String columnName = resolvedColumn;
            columnAlreadyExists = columnStrings.stream().anyMatch(s -> s.startsWith(columnName));
        } while (columnAlreadyExists);

        return resolvedColumn;
    }

    public Aggregation toAggregation() {
        return aggregation;
    }

    public Aggregation toFilteredCountAggregation() {
        return filteredCountAggregation;
    }
}