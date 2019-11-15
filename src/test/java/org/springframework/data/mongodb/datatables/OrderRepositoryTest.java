package org.springframework.data.mongodb.datatables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class OrderRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Before
    public void init() {
        productRepository.deleteAll();
    }

    private DataTablesInput getDefaultInput() {
        DataTablesInput input = new DataTablesInput();

        List<String> productRefColumns = new ArrayList<>();
        productRefColumns.add("label");
        productRefColumns.add("isEnabled");


        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("createdAt", true, true),
                createRefColumn("product", true, true, "product", productRefColumns)
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

    @Test
    public void referenceSearchable() {
        // Given
        Product p3 = Product.PRODUCT3;
        productRepository.save(Product.PRODUCT2);
        productRepository.save(p3);

        Order order = Order.ORDER2(p3);

        orderRepository.save(Order.ORDER1(Product.PRODUCT2));
        orderRepository.save(order);

        // When
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("product3", false));
        DataTablesOutput<Order> output = orderRepository.findAll(input);

        // Then
        assertThat(output.getData()).containsOnly(order);
    }

    @Test
    public void manualSpringQuery() {

        // Given
        Product p3 = Product.PRODUCT3;
        productRepository.save(Product.PRODUCT2);
        productRepository.save(p3);

        Order order = Order.ORDER2(p3);

        orderRepository.save(Order.ORDER1(Product.PRODUCT2));
        orderRepository.save(order);

        // When
        ProjectionOperation projectDbRefArr = Aggregation
                .project("label", "createdAt", "product", "_class")
                .and(ObjectOperators.ObjectToArray.valueOfToArray("product"))
                .as("product_fk_arr");

        ProjectionOperation projectDbRefObject = Aggregation
                .project("label", "createdAt", "product", "_class")
                .and( "product_fk_arr").arrayElementAt(1)
                .as("product_key_obj");

        ProjectionOperation projectPidField = Aggregation
                .project("label", "createdAt", "product", "_class")
                .and("product_key_obj.v").as("product_id");

        LookupOperation lookupOperation = Aggregation
                .lookup("product", "product_id", "_id", "product_resolved");

        MatchOperation matchOperation = Aggregation
                .match(Criteria.where("product_resolved.label").regex("product3", "i"));

        Aggregation agg = Aggregation.newAggregation(projectDbRefArr, projectDbRefObject, projectPidField, lookupOperation, matchOperation);

        AggregationResults<Order> data = mongoOperations.aggregate(agg, "order", Order.class);

        // Then
        assertThat(data.getMappedResults()).containsOnly(order);
    }
}
