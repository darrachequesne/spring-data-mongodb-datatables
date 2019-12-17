package org.springframework.data.mongodb.datatables;

import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class BenchmarkRunner {

    protected static ProductRepository productRepository;

    protected static OrderRepository orderRepository;

    protected static Order order2;

    @Test
    public void executeJmhRunner() throws RunnerException {
        createTestData();
        Options opt = new OptionsBuilder()
                .include("\\." + this.getClass().getSimpleName() + "\\.")
                .warmupIterations(5)
                .measurementIterations(10)
                .forks(0)
                .threads(1)
                .shouldDoGC(true)
                .shouldFailOnError(true)
                .resultFormat(ResultFormatType.JSON)
                .shouldFailOnError(true)
                .jvmArgs("-server")
                .build();

        new Runner(opt).run();
    }

    private void createTestData() {
        productRepository.deleteAll();
        orderRepository.deleteAll();

        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            products.add(Product.builder()
                    .id(i)
                    .label("Product " + i)
                    .isEnabled(i % 2 == 0)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        productRepository.saveAll(products);

        List<Order> orders = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            orders.add(Order.builder()
                    .id(i)
                    .label("Order " + i)
                    .isEnabled(i % 2 == 1)
                    .createdAt(LocalDateTime.now())
                    .product(products.get(i - 1))
                    .build());
        }

        orderRepository.saveAll(orders);

        order2 = orders.get(21);
    }
}
