package org.springframework.data.mongodb.datatables;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Document
@Data
@Builder
class Product {

    static Product PRODUCT1 = Product.builder()
            .id(1)
            .label("product1")
            .isEnabled(true)
            .createdAt(truncateToMillis(LocalDateTime.now()))
            .characteristic(new Characteristic("key1", "val11"))
            .characteristic(new Characteristic("key2", "val21"))
            .build();

    static Product PRODUCT2 = Product.builder()
            .id(2)
            .label("product2")
            .isEnabled(true)
            .createdAt(truncateToMillis(LocalDateTime.now().plusHours(1)))
            .characteristic(new Characteristic("key1", "val12"))
            .build();

    static Product PRODUCT3 = Product.builder()
            .id(3)
            .label("product3")
            .isEnabled(false)
            .createdAt(truncateToMillis(LocalDateTime.now().minusHours(1)))
            .characteristic(new Characteristic("key2", "val23"))
            .build();

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

    private boolean isEnabled;

    @Singular
    private List<Characteristic> characteristics;

    @Data
    public static class Characteristic {
        private final String key;
        private final String value;

        Characteristic(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}