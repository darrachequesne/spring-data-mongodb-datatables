package org.springframework.data.mongodb.datatables;

import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class Benchmarks extends BenchmarkRunner {

    @Autowired
    void setProductRepository(ProductRepository productRepository) {
        Benchmarks.productRepository = productRepository;
    }

    @Autowired
    void setOrderRepository(OrderRepository orderRepository) {
        Benchmarks.orderRepository = orderRepository;
    }

    @Benchmark
    public void globalFilterRef() {
        DataTablesInput input = getDefaultInputRef();
        input.setSearch(new DataTablesInput.Search(" ORDer 22  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
    }

    @Benchmark
    public void globalFilter() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" ORDer 22  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
    }

    @Benchmark
    public void globalFilter_additionalCriteria() {
        Criteria criteria = where("isEnabled").ne(true);

        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" ORDer 2  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input, criteria);
        assertThat(output.getData().size()).isEqualTo(6);
    }

    @Benchmark
    public void globalFilterRef_additionalCriteria() {
        Criteria criteria = where("isEnabled").ne(true);

        DataTablesInput input = getDefaultInputRef();
        input.setSearch(new DataTablesInput.Search(" ORDer 2  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input, criteria);
        assertThat(output.getData().size()).isEqualTo(6);
    }

    private DataTablesInput getDefaultInput() {
        DataTablesInput input = new DataTablesInput();
        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("isEnabled", true, true),
                createColumn("createdAt", true, true),
                createColumn("characteristics.key", true, true),
                createColumn("characteristics.value", true, true),
                createColumn("product", false, false)
        ));
        input.setSearch(new DataTablesInput.Search("", false));
        return input;
    }

    private DataTablesInput getDefaultInputRef() {
        DataTablesInput input = new DataTablesInput();

        List<String> productRefColumns = new ArrayList<>();
        productRefColumns.add("label");
        productRefColumns.add("isEnabled");

        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("isEnabled", true, true),
                createColumn("createdAt", true, true),
                createColumn("characteristics.key", true, true),
                createColumn("characteristics.value", true, true),
                createRefColumn("product", false, false, "product", productRefColumns)
        ));
        input.setSearch(new DataTablesInput.Search("", false));
        return input;
    }

    private DataTablesInput.Column createColumn(String columnName, boolean orderable, boolean searchable) {
        DataTablesInput.Column column = new DataTablesInput.Column();
        column.setData(columnName);
        column.setOrderable(orderable);
        column.setSearchable(searchable);
        column.setSearch(new DataTablesInput.Search("", true));
        return column;
    }

    private DataTablesInput.Column createRefColumn(String columnName, boolean orderable, boolean searchable, String refCollection,  List<String> refColumns) {
        DataTablesInput.Column column = new DataTablesInput.Column();
        column.setData(columnName);
        column.setOrderable(orderable);
        column.setSearchable(searchable);
        column.setReference(true);
        column.setReferenceCollection(refCollection);
        column.setReferenceColumns(refColumns);
        column.setSearch(new DataTablesInput.Search("", true));
        return column;
    }
}
