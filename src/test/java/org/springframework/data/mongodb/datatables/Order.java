package org.springframework.data.mongodb.datatables;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Document
@Data
@Builder
public class Order {

    static Order ORDER1(Product product) {
        return Order.builder()
                .id(1)
                .label("order1")
                .createdAt(truncateToMillis(LocalDateTime.now()))
                .productId(product.getId())
                .product(product)
                .build();
    }

    static Order ORDER2(Product product) {
        return Order.builder()
                .id(2)
                .label("order2")
                .createdAt(truncateToMillis(LocalDateTime.now()))
                .productId(product.getId())
                .product(product)
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

    private String label;

    private LocalDateTime createdAt;

    private long productId;

    @DBRef
    private Product product;
}
