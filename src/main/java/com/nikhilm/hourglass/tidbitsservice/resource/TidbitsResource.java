package com.nikhilm.hourglass.tidbitsservice.resource;

import com.nikhilm.hourglass.tidbitsservice.exceptions.TidbitException;
import com.nikhilm.hourglass.tidbitsservice.models.*;
import com.nikhilm.hourglass.tidbitsservice.repositories.TidbitFeedRepository;
import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import com.nikhilm.hourglass.tidbitsservice.services.TidbitsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RestController
@Slf4j
public class TidbitsResource {

    @Autowired
    TidbitsService tidbitsService;

    @Autowired
    TopicRepository topicRepository;


    @Autowired
    TidbitFeedRepository tidbitFeedRepository;

    @Autowired
    ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory;

    ReactiveCircuitBreaker rcb;

    TidbitsResource(ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory)    {
        this.reactiveCircuitBreakerFactory = reactiveCircuitBreakerFactory;
        rcb = reactiveCircuitBreakerFactory.create("tidbits");
    }

    @GetMapping("/tidbits")
    public Mono<Tidbits> getTidbits(@RequestHeader("user") Optional<String> user) {
        return rcb.run(tidbitFeedRepository.findByFeedDate(LocalDate.now()),
                throwable -> {
                    log.info("Exception thrown " + throwable.getMessage());
                    return Mono.error(new TidbitException(500, "Internal server error"));
                })
                .flatMap(tidbitFeed -> {
                    Tidbits tidbitResponse = new Tidbits();
                    tidbitResponse.getTriviaList().addAll(tidbitFeed.getTriviaList());
                    tidbitResponse.setFeedDate(LocalDate.now());
                    //retrieve user
                    // check favourites

                    if (user.isPresent()) {
                        String triviaParams = constructParam(tidbitResponse.getTriviaList());
                        log.info("triviaParams" + triviaParams);
                        return reactiveCircuitBreakerFactory.create("favourites")
                                .run(tidbitsService.fetchFavourites(user.get(), triviaParams),
                                        throwable -> {
                                            tidbitResponse.setFavouritesEnabled(false);
                                            return Mono.just(new FavouriteTriviaResponse());
                                        })
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
                }).switchIfEmpty(Mono.defer(() -> this.getNewFeed()))
                .onErrorMap(throwable -> new TidbitException(500, "Internal server error!"));
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
        return tidbitsService.groupByCategory()
            .flatMap(topicStats -> {
                return pickTopicIndex(topicStats);
            })
            .flatMap(topicStats -> {
                return rcb.run(topicRepository.findByCategory(topicStats.getCategory())
                        .skip(topicStats.getCount())
                        .take(1), throwable -> Flux.error(new TidbitException(500, "Internal server error!")));
            })
            .flatMap(topic -> {
                //call datamuse api
                log.info("topic " + topic.getCategory() + " " + topic.getWord());
                return tidbitsService.getWords(topic.getWord())
                   .flatMap(list -> {
                       log.info("does it get here by any chance " + list);
                        int idx = getRandomNumberinRange(0, list.size() - 1);
                        log.info("Selected word [" + idx + "] " + list.stream().skip(idx).limit(1).findAny());
                        LinkedHashMap word = (LinkedHashMap) (list.stream().skip(idx).limit(1).findAny()
                                       .orElse(getDefault(topic.getCategory().getValue())));

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
                log.info("saved trivia and feed " + feed);
                return feed;
            })
            .flatMap(tidbitFeed1 -> rcb
            .run(tidbitFeedRepository.save(tidbitFeed1), throwable -> Mono.just(tidbitFeed1)))
            .flatMap(tidbitFeed1 -> {
                Tidbits response = new Tidbits();
                response.getTriviaList().addAll(tidbitFeed1.getTriviaList());
                response.setFeedDate(LocalDate.now());
                log.info("response " + response);
                return Mono.just(response);
            });
    }



    private LinkedHashMap<Object, Object> getDefault(String category) {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        switch (category)  {
            case "science":
                result.put("word", "Science trivia");
                break;
            case "technology":
                result.put("word", "Technology trivia");
                break;
            case "sport":
                result.put("word", "Sport trivia");
                break;
            case "travel":
                result.put("word", "Travel trivia");
                break;
            default:
                result.put("word", "");

        }
        return result;

    }

    private Mono<TopicStats> pickTopicIndex(TopicStats stats)   {
        TopicStats topicStats = new TopicStats();
        topicStats.setCategory(stats.getCategory());
        topicStats.setCount(getRandomNumberinRange(0, stats.getCount() - 1));
        return Mono.just(topicStats);
    }

    private int getRandomNumberinRange(int min, int max) {
        return new SecureRandom().ints(min, max + 1).findFirst().getAsInt();

    }



}
