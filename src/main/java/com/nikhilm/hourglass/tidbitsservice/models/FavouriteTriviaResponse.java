package com.nikhilm.hourglass.tidbitsservice.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FavouriteTriviaResponse {
    String userId;
    List<Trivia> favouriteTrivia = new ArrayList<>();
}
