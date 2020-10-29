package com.nikhilm.hourglass.tidbitsservice.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Document(collection = "triviafeeds")
public class TidbitFeed {
    @Id
    private String id;
    private List<Trivia> triviaList = new ArrayList<>();
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate feedDate;
}
