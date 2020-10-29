package com.nikhilm.hourglass.tidbitsservice.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Trivia {
    private String term;
    private String fact;
    private Category category;
    private boolean isFavourite = false;

}
