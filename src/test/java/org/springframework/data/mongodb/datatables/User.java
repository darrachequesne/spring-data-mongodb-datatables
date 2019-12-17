package org.springframework.data.mongodb.datatables;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@Builder
public class User {

    static User USER1 = User.builder()
            .id(1)
            .firstName("FName")
            .lastName("LName")
            .build();

    @Id
    private long id;

    private String firstName;

    private String lastName;
}
