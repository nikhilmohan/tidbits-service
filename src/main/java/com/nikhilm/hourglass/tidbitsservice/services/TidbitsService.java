package com.nikhilm.hourglass.tidbitsservice.services;

import com.nikhilm.hourglass.tidbitsservice.exceptions.TidbitException;
import com.nikhilm.hourglass.tidbitsservice.models.FavouriteTriviaResponse;
import com.nikhilm.hourglass.tidbitsservice.models.Topic;
import com.nikhilm.hourglass.tidbitsservice.models.TopicStats;
import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Service
@Slf4j
public class TidbitsService {


    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @Autowired
    WebClient webClient;

    @Value("${service.url.gateway}")
    private String gatewayServiceUrl;


    public Mono<List> getWords(String topic)  {
        log.info("Tidbits service to fetch words " + topic);
        return webClient.get().uri("https://api.datamuse.com/words?topics=" + topic)
                .exchange()
                .flatMap(clientResponse -> {
                    log.info("Datamuse response status " + clientResponse.statusCode().toString() + " topic " + topic);
                    if (!clientResponse.statusCode().is2xxSuccessful())  {
                        return Mono.error(new TidbitException(502, "External service unavailable!"));
                    }
                    else return clientResponse.bodyToMono(List.class);
                });

    }

    public Mono<FavouriteTriviaResponse> fetchFavourites(String userId, String params)   {
        return callFavourites(userId, params)
                .flatMap(clientResponse -> {
                    if (!clientResponse.statusCode().is2xxSuccessful())  {
                        return Mono.error(new RuntimeException("Favourites service error!"));
                    }
                    else return clientResponse.bodyToMono(FavouriteTriviaResponse.class);
                });
    }

    private Mono<ClientResponse> callFavourites(String userId, String params)    {
        return webClient.get()
                .uri("http://" + gatewayServiceUrl + ":9900/favourites-service/favourites/user/" + userId + "/trivia?ids="+params)
                .exchange();

    }


    public Flux<TopicStats> groupByCategory() {
        GroupOperation groupByCategoryAndCount = group("category")
                .count().as("count");

        Aggregation aggregation = newAggregation(groupByCategoryAndCount);
        return mongoTemplate.aggregate(aggregation, Topic.class, TopicStats.class);
    }


}
