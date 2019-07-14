package ru.masterjava.persist.model;

import lombok.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class City extends RefEntity {
    private @NonNull String name;

    public City(String ref, String name) {
        super(ref);
        this.name = name;
    }
}
