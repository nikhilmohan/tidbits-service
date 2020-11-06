package com.nikhilm.hourglass.tidbitsservice.resource;

import com.nikhilm.hourglass.tidbitsservice.models.*;
import com.nikhilm.hourglass.tidbitsservice.repositories.TidbitFeedRepository;
import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import com.nikhilm.hourglass.tidbitsservice.services.TidbitsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RestController
@Slf4j
public class TidbitsResource {
    @Autowired
    private TidbitsService tidbitsService;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @Autowired
    TidbitFeedRepository tidbitFeedRepository;

    @GetMapping("/tidbits")
    public Mono<Tidbits> getTidbits(@RequestHeader("user") Optional<String> user) {
        return tidbitFeedRepository.findByFeedDate(LocalDate.now())
                .flatMap(tidbitFeed -> {
                    Tidbits tidbitResponse = new Tidbits();
                    tidbitResponse.getTriviaList().addAll(tidbitFeed.getTriviaList());
                    tidbitResponse.setFeedDate(LocalDate.now());
                    //retrieve user
                    // check favourites

                    if (user.isPresent()) {
                        String triviaParams = constructParam(tidbitResponse.getTriviaList());
                        log.info("triviaParams" + triviaParams);
                        WebClient client = WebClient.create("http://localhost:9900/favourites-service/favourites/user/"
                                + user.get() + "/trivia");
                        return client.get().uri("?ids=" + triviaParams)
                                .retrieve()
                                .bodyToMono(FavouriteTriviaResponse.class)
                                .flatMap(favouriteTriviaResponse -> {
                                    tidbitResponse.setTriviaList(tidbitResponse.getTriviaList().stream()
                                            .map(trivia -> {
                                                trivia.setFavourite(isFavouriteTrivia(trivia.getTerm(), favouriteTriviaResponse));
                                                return trivia;
                                            })
                                            .collect(Collectors.toList()));
                                    return Mono.just(tidbitResponse);

                                });
                    } else return Mono.just(tidbitResponse);
                }).switchIfEmpty(Mono.defer(() -> this.getNewFeed()));
    }

    private boolean isFavouriteTrivia(String term, FavouriteTriviaResponse favouriteTriviaResponse) {
        return favouriteTriviaResponse.getFavouriteTrivia().stream()
                .anyMatch(trivia -> trivia.getTerm().equalsIgnoreCase(term));
    }

    private String constructParam(List<Trivia> triviaList) {
        return triviaList.stream()
                .map(trivia -> trivia.getTerm())
                .reduce((s, s2) -> s + "," + s2).orElse("");
    }
    public Mono<Tidbits> getNewFeed()   {
        log.info("Getting new feed!");
        TidbitFeed tidbitFeed = new TidbitFeed();
        return groupByCategory()
            .flatMap(topicStats -> {
                return pickTopicIndex(topicStats);
            })
            .flatMap(topicStats -> {
                return topicRepository.findByCategory(topicStats.getCategory())
                        .skip(topicStats.getCount())
                        .take(1);
            })
            .flatMap(topic -> {
                //call datamuse api
                log.info("topic " + topic.getCategory() + " " + topic.getWord());
                WebClient client = WebClient.create("https://api.datamuse.com/words");
                return client.get().uri("?topics="+topic.getWord())
                        .retrieve()
                        .bodyToMono(List.class)
                        .flatMap(list -> {
                            int idx = getRandomNumberinRange(0, list.size() - 1);
                            log.info("Selected word [" + idx + "] " + list.stream().skip(idx).limit(1).findAny());

                            LinkedHashMap word = (LinkedHashMap) (list.stream().skip(idx).limit(1).findAny().get());
                            log.info("Word " + word.get("word"));
                            Trivia trivia = new Trivia();
                            trivia.setCategory(topic.getCategory());
                            trivia.setTerm(word.get("word").toString());
                            trivia.setFact("Some dummy gyan!");
                            return Mono.just(trivia);
//                                WebClient apiClient = WebClient.create("http://localhost:8099/");
//                                return apiClient.get().uri(word.getWord())
//                                        .retrieve()
//                                        .bodyToMono(Trivia.class);
                        });
            })
            .reduce(tidbitFeed, (feed, trivia) -> {
                feed.setFeedDate(LocalDate.now());
                feed.getTriviaList().add(trivia);
                return feed;
            })
            .flatMap(tidbitFeedRepository::save)
            .flatMap(tidbitFeed1 -> {
                Tidbits response = new Tidbits();
                response.getTriviaList().addAll(tidbitFeed1.getTriviaList());
                response.setFeedDate(LocalDate.now());
                return Mono.just(response);
            });
    }
    private Mono<TopicStats> pickTopicIndex(TopicStats stats)   {
        TopicStats topicStats = new TopicStats();
        topicStats.setCategory(stats.getCategory());
        topicStats.setCount(getRandomNumberinRange(0, stats.getCount() - 1));
        return Mono.just(topicStats);
    }

    private int getRandomNumberinRange(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
    @GetMapping("/test")
    public Flux<Topic> getTopics()  {
        return topicRepository.findAll();
    }
    private Flux<TopicStats> groupByCategory() {
        GroupOperation groupByCategoryAndCount = group("category")
                .count().as("count");

        Aggregation aggregation = newAggregation(groupByCategoryAndCount);
        return mongoTemplate.aggregate(aggregation, Topic.class, TopicStats.class);
    }
}
