package org.springframework.data.mongodb.datatables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Includes the same tests as ProductRepositoryTest and more. Tests the functionality against the aggregation pipeline.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class OrderRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private Order order1;
    private Order order2;
    private Order order3;
    private Order order4;

    @Before
    public void init() {
        productRepository.deleteAll();
        orderRepository.deleteAll();

        productRepository.save(Product.PRODUCT1);
        productRepository.save(Product.PRODUCT2);
        productRepository.save(Product.PRODUCT3);
        productRepository.save(Product.PRODUCT4);

        userRepository.save(User.USER1);

        order1 = Order.ORDER1(Product.PRODUCT1);
        order2 = Order.ORDER2(Product.PRODUCT2);
        order3 = Order.ORDER3(Product.PRODUCT3);
        order4 = Order.ORDER4(Product.PRODUCT4, User.USER1);

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);
        orderRepository.save(order4);
    }

    private DataTablesInput getDefaultInput() {
        DataTablesInput input = new DataTablesInput();

        List<String> productRefColumns = new ArrayList<>();
        productRefColumns.add("label");
        productRefColumns.add("isEnabled");
        productRefColumns.add("createdAt");

        List<String> userRefColumns = new ArrayList<>();
        userRefColumns.add("firstName");
        userRefColumns.add("lastName");

        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("isEnabled", true, true),
                createColumn("createdAt", true, true),
                createColumn("characteristics.key", true, true),
                createColumn("characteristics.value", true, true),
                createRefColumn("product", true, true, "product", productRefColumns, "createdAt"),
                createRefColumn("user", true, true, "user", userRefColumns, "firstName")
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

    private DataTablesInput.Column createRefColumn(String columnName, boolean orderable, boolean searchable, String refCollection,  List<String> refColumns, String orderColumn) {
        DataTablesInput.Column column = new DataTablesInput.Column();
        column.setData(columnName);
        column.setOrderable(orderable);
        column.setSearchable(searchable);
        column.setReference(true);
        column.setReferenceCollection(refCollection);
        column.setReferenceColumns(refColumns);
        column.setReferenceOrderColumn(orderColumn);
        column.setSearch(new DataTablesInput.Search("", true));
        return column;
    }

    @Test
    public void referenceSearchable() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("product3", false));
        DataTablesOutput<Order> output = orderRepository.findAll(input);

        assertThat(output.getData()).containsOnly(order3);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void basic() {
        DataTablesOutput<Order> output = orderRepository.findAll(getDefaultInput());
        assertThat(output.getDraw()).isEqualTo(1);
        assertThat(output.getRecordsFiltered()).isEqualTo(4);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).containsOnly(order1, order2, order3, order4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void paginated() {
        DataTablesInput input = getDefaultInput();
        input.setDraw(2);
        input.setLength(1);
        input.setStart(1);

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getDraw()).isEqualTo(2);
        assertThat(output.getRecordsFiltered()).isEqualTo(4);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void sortAscending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.asc)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order3, order1, order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void sortDescending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.desc)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order2, order1, order3);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void globalFilter() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" ORDer2  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void globalFilter_empty_result() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" axb  ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData().size()).isEqualTo(0);
        assertThat(output.getError()).isNullOrEmpty();
    }

    @Test
    public void globalFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("^o\\w+der2$", true));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void columnFilter() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search(" ORDer3  ", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order3);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void columnFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("^o\\w+der3$", true)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order3);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void booleanAttribute() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("isEnabled").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("true", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order1, order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void empty() {
        DataTablesInput input = getDefaultInput();
        input.setLength(0);

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getRecordsFiltered()).isEqualTo(0);
        assertThat(output.getData()).hasSize(0);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void all() {
        DataTablesInput input = getDefaultInput();
        input.setLength(-1);

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getRecordsFiltered()).isEqualTo(4);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).hasSize(4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void subDocument() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("characteristics.key").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("key1", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order1, order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void converter() {
        DataTablesOutput<String> output = orderRepository.findAll(getDefaultInput(), Order::getLabel);
        assertThat(output.getData()).containsOnly("order1", "order2", "order3", "order4");
        assertThat(output.getError()).isNull();
    }

    @Test
    public void additionalCriteria() {
        Criteria criteria = where("label").in("order1", "order2");

        DataTablesOutput<Order> output = orderRepository.findAll(getDefaultInput(), criteria);
        assertThat(output.getRecordsFiltered()).isEqualTo(2);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).containsOnly(order1, order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void preFilteringCriteria() {
        Criteria criteria = where("label").in("order2", "order3");

        DataTablesOutput<Order> output = orderRepository.findAll(getDefaultInput(), null, criteria);
        assertThat(output.getRecordsFiltered()).isEqualTo(2);
        assertThat(output.getRecordsTotal()).isEqualTo(2);
        assertThat(output.getData()).containsOnly(order2, order3);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void columnNotSearchable() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column -> {
            column.setSearch(new DataTablesInput.Search(" PROduct3  ", false));
            column.setSearchable(false);
        });

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order1, order2, order3, order4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void columnNotOrderable() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.asc)));
        input.getColumn("createdAt").ifPresent(column ->
                column.setOrderable(false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order1, order2, order3);
        assertThat(output.getError()).isNull();
    }

    /**
     * Should be sorted by a special ref-sortable column name
     */
    @Test
    public void ref_sortAscending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(6, DataTablesInput.Order.Direction.asc)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order3, order1, order2, order4);
        assertThat(output.getError()).isNull();
    }

    /**
     * Should be sorted by a special ref-sortable column name
     */
    @Test
    public void ref_sortDescending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(6, DataTablesInput.Order.Direction.desc)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order4, order2, order1, order3);
    }

    @Test
    public void ref_paginated_sortDescending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(6, DataTablesInput.Order.Direction.desc)));
        input.setDraw(2);
        input.setLength(1);
        input.setStart(2);

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getDraw()).isEqualTo(2);
        assertThat(output.getError()).isNull();
        assertThat(output.getRecordsFiltered()).isEqualTo(4);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).containsOnly(order1);
    }

    @Test
    public void ref_paginated_sortAscending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(6, DataTablesInput.Order.Direction.asc)));
        input.setDraw(2);
        input.setLength(1);
        input.setStart(2);

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getDraw()).isEqualTo(2);
        assertThat(output.getError()).isNull();
        assertThat(output.getRecordsFiltered()).isEqualTo(4);
        assertThat(output.getRecordsTotal()).isEqualTo(4);
        assertThat(output.getData()).containsOnly(order2);
    }

    @Test
    public void ref_globalFilter() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("product2", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_globalFilter_user() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("FName", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_globalFilter_product4() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("product4", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_globalFilter_contains() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" PrODUct2 ", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_globalFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("^p\\w+uct2$", true));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order2);
        assertThat(output.getError()).isNull();
    }

    /**
     * Searches all reference columns of the specified reference column
     */
    @Test
    public void ref_columnFilter() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("product").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search(" PROduct3  ", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order3);
        assertThat(output.getError()).isNull();
    }

    /**
     * Searches all reference columns of the specified reference column
     */
    @Test
    public void ref_columnFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("product").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("^p\\w+uct3$", true)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order3);
        assertThat(output.getError()).isNull();
    }

    /**
     * Searches all reference columns of the specified reference column
     */
    @Test
    public void ref_booleanAttribute() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("product").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("true", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order1, order2);
        assertThat(output.getError()).isNull();
    }

    /**
     * Not supported -> handle as if column does not exist
     */
    @Test
    public void ref_subDocument() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("product.isEnabled").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("true", false)));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData().size()).isEqualTo(4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_converter() {
        DataTablesOutput<Product> output = orderRepository.findAll(getDefaultInput(), Order::getProduct);

        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2, Product.PRODUCT3, Product.PRODUCT4);
        assertThat(output.getError()).isNull();
    }

    /**
     * Currently not supported for reference columns
     */
    @Test
    public void ref_additionalCriteria() {
        Criteria criteria = where("product").in("product1", "product2");

        DataTablesOutput<Order> output = orderRepository.findAll(getDefaultInput(), criteria);
        assertThat(output.getError()).startsWith(new IllegalArgumentException().toString());
    }

    /**
     * Currently not supported for reference columns
     */
    @Test
    public void ref_preFilteringCriteria() {
        Criteria criteria = where("product").in("product2", "product3");

        DataTablesOutput<Order> output = orderRepository.findAll(getDefaultInput(), null, criteria);
        assertThat(output.getError()).startsWith(new IllegalArgumentException().toString());
    }

    @Test
    public void ref_columnNotSearchable() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("product").ifPresent(column -> {
            column.setSearch(new DataTablesInput.Search(" PROduct3  ", false));
            column.setSearchable(false);
        });

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(order1, order2, order3, order4);
        assertThat(output.getError()).isNull();
    }

    @Test
    public void ref_columnNotOrderable() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(6, DataTablesInput.Order.Direction.asc)));
        input.getColumn("product").ifPresent(column ->
                column.setOrderable(false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsSequence(order1, order2, order3);
        assertThat(output.getError()).isNull();
    }
}
