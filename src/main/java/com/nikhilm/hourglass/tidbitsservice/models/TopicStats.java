package com.nikhilm.hourglass.tidbitsservice.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class TopicStats  {
    @Id
    private Category category;
    private int count;

}
