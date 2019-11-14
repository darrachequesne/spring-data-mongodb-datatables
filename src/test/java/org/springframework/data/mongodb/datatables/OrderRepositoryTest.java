package org.springframework.data.mongodb.datatables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class OrderRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Before
    public void init() {
        productRepository.deleteAll();

        productRepository.save(Product.PRODUCT1);
        productRepository.save(Product.PRODUCT2);
        orderRepository.save(Order.ORDER1(Product.PRODUCT1));
        orderRepository.save(Order.ORDER2(Product.PRODUCT2));
    }

    private DataTablesInput getDefaultInput() {
        DataTablesInput input = new DataTablesInput();
        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("createdAt", true, true),
                createColumn("product", true, true)
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

    @Test
    public void referenceSearchable() {
        DataTablesInput input = getDefaultInput();

        input.setSearch(new DataTablesInput.Search("product2", false));

        DataTablesOutput<Order> output = orderRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Order.ORDER2(Product.PRODUCT2));
    }
}
