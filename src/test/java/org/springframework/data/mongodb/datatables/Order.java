package org.springframework.data.mongodb.datatables;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Document
@Data
@Builder
public class Order {

    static Order ORDER1(Product product) {
        return Order.builder()
                .id(1)
                .label("order1")
                .isEnabled(true)
                .createdAt(truncateToMillis(LocalDateTime.now()))
                .product(product)
                .characteristic(new Product.Characteristic("key1", "val11"))
                .characteristic(new Product.Characteristic("key2", "val21"))
                .build();
    }

    static Order ORDER2(Product product) {
        return Order.builder()
                .id(2)
                .label("order2")
                .isEnabled(true)
                .createdAt(truncateToMillis(LocalDateTime.now().plusHours(1)))
                .product(product)
                .characteristic(new Product.Characteristic("key1", "val12"))
                .build();
    }

    static Order ORDER3(Product product) {
        return Order.builder()
                .id(3)
                .label("order3")
                .isEnabled(false)
                .createdAt(truncateToMillis(LocalDateTime.now().minusHours(1)))
                .product(product)
                .characteristic(new Product.Characteristic("key2", "val23"))
                .build();
    }

    /**
     * Since JDK 9, LocalDateTime uses a precision of nanoseconds, while the BSON dates in MongoDB have a millisecond
     * precision, so we have to truncate it in order not to lose information.
     *
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8068730">https://bugs.openjdk.java.net/browse/JDK-8068730</a>
     * @see <a href="http://bsonspec.org/spec.html">http://bsonspec.org/spec.html</a>
     */
    private static LocalDateTime truncateToMillis(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.MILLIS);
    }

    @Id
    private long id;

    private boolean isEnabled;

    private String label;

    private LocalDateTime createdAt;

    @Singular
    private List<Product.Characteristic> characteristics;

    @DBRef
    private Product product;
}
