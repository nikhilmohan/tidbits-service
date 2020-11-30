package com.nikhilm.hourglass.tidbitsservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhilm.hourglass.tidbitsservice.exceptions.TidbitException;
import com.nikhilm.hourglass.tidbitsservice.models.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@ExtendWith(MockitoExtension.class)
@Slf4j
class TidbitsServiceTest {

    @Mock
    ReactiveMongoTemplate mongoTemplate;

    @Mock
    WebClient webClient;

    @InjectMocks
    TidbitsService tidbitsService;

    @Test
    public void testGetTriviaWithFavourites()   {

        FavouriteTriviaResponse favouriteTriviaResponse = new FavouriteTriviaResponse();
        favouriteTriviaResponse.setUserId("abc");
        Trivia trivia =  new Trivia("blockchain", "innovation", Category.technology, false);
        favouriteTriviaResponse.getFavouriteTrivia().add(trivia);

        ObjectMapper mapper = new ObjectMapper();
        String body = "";
            try {
            body =  mapper.writeValueAsString(favouriteTriviaResponse);
        } catch (
        JsonProcessingException e) {
            log.error("Cannot parse");
        }


        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body).build();

        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString()))
                .thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));
            StepVerifier.create(tidbitsService.fetchFavourites("abc",
                    "5f9672a3d20d3846ef8cd39d,5f96741231a5f039c69e7235,5f967471cd5fdb67817a8f1c"))
                    .expectSubscription()
                    .expectNextMatches(response -> response.getFavouriteTrivia().size() == 1
                                        && response.getFavouriteTrivia().stream()
                                            .anyMatch(trivia1 -> trivia1.getCategory().equals(Category.technology)
                                        && trivia1.getTerm().equalsIgnoreCase("blockchain")))
                    .verifyComplete();
    }

    @Test
    public void testGetMoviesWithNoFavouritesService() {

        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();
        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString()))
                .thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));
        StepVerifier.create(tidbitsService.fetchFavourites("abc", ""))
                .expectSubscription()
                .expectError()
                .verify();
    }

    @Test
    public void testGetWords()  {

        LinkedHashMap lhm1 = new LinkedHashMap();
        lhm1.put("word", "blockchain");
        LinkedHashMap lhm2 = new LinkedHashMap();
        lhm2.put("word", "deep learning");



        ObjectMapper mapper = new ObjectMapper();
        String body = "";
        try {
            body =  mapper.writeValueAsString(List.of(lhm1, lhm2));
        } catch (
                JsonProcessingException e) {
            log.error("Cannot parse");
        }
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body).build();


        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));
        StepVerifier.create(tidbitsService.getWords("technology"))
                .expectSubscription()
                .expectNextMatches(list -> list.size() == 2)
                .verifyComplete();



    }

    @Test
    public void testGetWordsAPIDown()  {

        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();

        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));

        StepVerifier.create(tidbitsService.getWords("technology"))
                .expectSubscription()
                .expectError()
                .verify();



    }

    @Test
    public void testGroupByCategory()   {

        List<TopicStats> topicStatsFlux = Arrays.asList(new TopicStats(Category.science, 1),
                new TopicStats(Category.technology, 1), new TopicStats(Category.sport, 1),
                new TopicStats(Category.travel, 1));


        GroupOperation groupByCategoryAndCount = group("category")
                .count().as("count");

        Aggregation aggregation = newAggregation(groupByCategoryAndCount);

//        when(mongoTemplate.aggregate(aggregation,Topic.class,TopicStats.class)).thenReturn(Flux.fromIterable(topicStatsFlux));

        doReturn(Flux.fromIterable(topicStatsFlux)).when(mongoTemplate).aggregate(any(Aggregation.class), eq(Topic.class),
                eq(TopicStats.class));
        StepVerifier.create(tidbitsService.groupByCategory())
                .expectSubscription()
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .verifyComplete();
    }

}