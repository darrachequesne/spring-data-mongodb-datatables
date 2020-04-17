package org.springframework.data.mongodb.datatables;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

final class DataTablesCriteria {

    private final DataTablesInput input;
    private final Criteria additionalCriteria;
    private final Criteria preFilteringCriteria;

    DataTablesCriteria(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria) {
        this.input = input;
        this.additionalCriteria = additionalCriteria;
        this.preFilteringCriteria = preFilteringCriteria;
    }

    Query toQuery() {
        Query query = this.toQueryWithoutLimit();
        addSort(query, input);

        return query;
    }

    Query toQueryWithoutLimit() {
        Query query = new Query();

        addGlobalCriteria(query, input);
        input.getColumns().forEach(column -> this.addColumnCriteria(query, column));

        if (additionalCriteria != null) {
            query.addCriteria(additionalCriteria);
        }
        if (preFilteringCriteria != null) {
            query.addCriteria(preFilteringCriteria);
        }

        return query;
    }

    private void addGlobalCriteria(Query query, DataTablesInput input) {
        if (!hasText(input.getSearch().getValue())) return;

        Criteria[] criteriaArray = input.getColumns().stream()
                .filter(DataTablesInput.Column::isSearchable)
                .map(column -> createCriteria(column, input.getSearch()))
                .toArray(Criteria[]::new);

        if (criteriaArray.length == 1) {
            query.addCriteria(criteriaArray[0]);
        } else if (criteriaArray.length >= 2) {
            query.addCriteria(new Criteria().orOperator(criteriaArray));
        }
    }

    private void addColumnCriteria(Query query, DataTablesInput.Column column) {
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

    private void addSort(Query query, DataTablesInput input) {
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
}