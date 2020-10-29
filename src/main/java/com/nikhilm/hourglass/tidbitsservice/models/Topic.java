package com.nikhilm.hourglass.tidbitsservice.models;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Document(collection = "topics")
public class Topic {
    private String word;
    private Category category;
}
