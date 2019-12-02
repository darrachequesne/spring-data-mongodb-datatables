package org.springframework.data.mongodb.datatables;

import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public abstract class BenchmarkRunner {

    protected static ProductRepository productRepository;

    protected static OrderRepository orderRepository;

    protected static Order order1;
    protected static Order order2;
    protected static Order order3;

    @Test
    public void executeJmhRunner() throws RunnerException {
        createTestData();
        Options opt = new OptionsBuilder()
                // set the class name regex for benchmarks to search for to the current class
                .include("\\." + this.getClass().getSimpleName() + "\\.")
                .warmupIterations(5)
                .measurementIterations(50)
                // do not use forking or the benchmark methods will not see references stored within its class
                .forks(0)
                // do not use multiple threads
                .threads(1)
                .shouldDoGC(true)
                .shouldFailOnError(true)
                .resultFormat(ResultFormatType.JSON)
                //.result("report.txt") // set this to a valid filename if you want reports
                .shouldFailOnError(true)
                .jvmArgs("-server")
                .build();

        new Runner(opt).run();
    }

    private void createTestData() {
        productRepository.deleteAll();
        orderRepository.deleteAll();

        productRepository.save(Product.PRODUCT1);
        productRepository.save(Product.PRODUCT2);
        productRepository.save(Product.PRODUCT3);

        order1 = Order.ORDER1(Product.PRODUCT1);
        order2 = Order.ORDER2(Product.PRODUCT2);
        order3 = Order.ORDER3(Product.PRODUCT3);

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);
    }
}
