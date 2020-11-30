package com.nikhilm.hourglass.tidbitsservice.resource;

import com.nikhilm.hourglass.tidbitsservice.exceptions.ApiError;
import com.nikhilm.hourglass.tidbitsservice.exceptions.TidbitException;
import com.nikhilm.hourglass.tidbitsservice.models.*;
import com.nikhilm.hourglass.tidbitsservice.repositories.TidbitFeedRepository;
import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import com.nikhilm.hourglass.tidbitsservice.services.TidbitsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


@WebFluxTest
class TidbitsResourceTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    TidbitsService tidbitsService;

    @MockBean
    TopicRepository topicRepository;


    @MockBean
    TidbitFeedRepository tidbitFeedRepository;


    private static TidbitFeed tidbitFeed = new TidbitFeed();

    @BeforeAll
    public static void setup() {

        tidbitFeed.getTriviaList().addAll(Arrays.asList(new Trivia("football", "world game", Category.sport, false),
                new Trivia("computer", "innovation", Category.technology, false),
                new Trivia("destination", "Plitvice National Park is awesome!", Category.travel, false),
                new Trivia("thermodynamics", "relationship between heat and other forms of energy", Category.science, false)));
        tidbitFeed.setFeedDate(LocalDate.now());

    }


    @Test
    void getTidbitsWithoutUser() {
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(tidbitFeed));
        Tidbits tidbits = webTestClient.get().uri("http://localhost:9050/tidbits")
                .exchange()
                .expectBody(Tidbits.class)
                .returnResult()
                .getResponseBody();
        assertEquals(LocalDate.now(), tidbits.getFeedDate());
        assertEquals(4, tidbits.getTriviaList().size());
        assertEquals("Plitvice National Park is awesome!", tidbits.getTriviaList()
                .stream().filter(trivia -> trivia.getTerm().equalsIgnoreCase("destination")).findAny().get().getFact());


    }

    @Test
    void getTidbitsWithUser() {
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(tidbitFeed));
        when(tidbitsService.fetchFavourites(eq("abc"), anyString())).thenReturn(Mono.just(new FavouriteTriviaResponse()));
        Tidbits tidbits = webTestClient.get().uri("http://localhost:9050/tidbits")
                .header("user", "abc")
                .exchange()
                .expectBody(Tidbits.class)
                .returnResult()
                .getResponseBody();
        assertEquals(LocalDate.now(), tidbits.getFeedDate());
        assertEquals(4, tidbits.getTriviaList().size());
        assertEquals("Plitvice National Park is awesome!", tidbits.getTriviaList()
                .stream().filter(trivia -> trivia.getTerm().equalsIgnoreCase("destination")).findAny().get().getFact());


    }

    @Test
    void getTidbitsWithUserHavingFavourites() {
        FavouriteTriviaResponse favouriteTriviaResponse = new FavouriteTriviaResponse();
        favouriteTriviaResponse.setUserId("abc");
        favouriteTriviaResponse
                .getFavouriteTrivia()
                .add(new Trivia("destination", "Plitvice National Park is awesome!", Category.travel, true));
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(tidbitFeed));
        when(tidbitsService.fetchFavourites(eq("abc"), anyString())).thenReturn(Mono.just(favouriteTriviaResponse));
        Tidbits tidbits = webTestClient.get().uri("http://localhost:9050/tidbits")
                .header("user", "abc")
                .exchange()
                .expectBody(Tidbits.class)
                .returnResult()
                .getResponseBody();
        assertEquals(LocalDate.now(), tidbits.getFeedDate());
        assertEquals(1, tidbits.getTriviaList().stream().filter(trivia -> trivia.isFavourite()).count());
        assertEquals("Plitvice National Park is awesome!", tidbits.getTriviaList()
                .stream().filter(trivia -> trivia.isFavourite()).findAny().get().getFact());


    }
    @Test
    void getTidbitsWithFavouritesException()    {
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(tidbitFeed));
        when(tidbitsService.fetchFavourites(eq("abc"), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Favourites service error!")));
        Tidbits tidbits = webTestClient.get().uri("http://localhost:9050/tidbits")
                .header("user", "abc")
                .exchange()
                .expectBody(Tidbits.class)
                .returnResult()
                .getResponseBody();
        assertEquals(LocalDate.now(), tidbits.getFeedDate());
        assertEquals(4, tidbits.getTriviaList().size());
        assertEquals("Plitvice National Park is awesome!", tidbits.getTriviaList()
                .stream().filter(trivia -> trivia.getTerm().equalsIgnoreCase("destination")).findAny().get().getFact());

    }
    @Test
    void getTidbitsWithDBDown() {
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.error(new RuntimeException("DB down")));
        webTestClient.get().uri("http://localhost:9050/tidbits")
                .header("user", "abc")
                .exchange()
                .expectBody(ApiError.class);


    }
    @Test
    void testGetNewFeed()   {


        List<TopicStats> topicStatsList = Arrays.asList(new TopicStats(Category.science, 1),
                new TopicStats(Category.travel, 1), new TopicStats(Category.sport, 1),
                new TopicStats(Category.technology, 1));
        when(tidbitFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.empty());
        when(tidbitsService.groupByCategory()).thenReturn(Flux.fromIterable(topicStatsList));
        when(topicRepository.findByCategory(Category.technology))
                .thenReturn(Flux.just(new Topic("computer", Category.technology)));
        when(topicRepository.findByCategory(Category.science))
                .thenReturn(Flux.just(new Topic("thermodynamics", Category.science)));
        when(topicRepository.findByCategory(Category.sport))
                .thenReturn(Flux.just(new Topic("cricket", Category.sport)));
        when(topicRepository.findByCategory(Category.travel))
                .thenReturn(Flux.just(new Topic("plitvice", Category.travel)));

        LinkedHashMap lhm1 = new LinkedHashMap();
        lhm1.put("word","computer");
        LinkedHashMap lhm2 = new LinkedHashMap();
        lhm2.put("word","thermodynamics");
        LinkedHashMap lhm3 = new LinkedHashMap();
        lhm3.put("word","cricket");
        LinkedHashMap lhm4 = new LinkedHashMap();
        lhm4.put("word","plitvice");

        when(tidbitsService.getWords("computer")).thenReturn(Mono.just(List.of(lhm1)));
        when(tidbitsService.getWords("thermodynamics")).thenReturn(Mono.just(List.of(lhm2)));
        when(tidbitsService.getWords("cricket")).thenReturn(Mono.just(List.of(lhm3)));
        when(tidbitsService.getWords("plitvice")).thenReturn(Mono.just(List.of(lhm4)));
        ArgumentCaptor<TidbitFeed> captor = ArgumentCaptor.forClass(TidbitFeed.class);

        when(tidbitFeedRepository.save(captor.capture())).thenReturn(Mono.just(tidbitFeed));

        Tidbits tidbits = webTestClient.get().uri("http://localhost:9050/tidbits")
                .exchange()
                .expectBody(Tidbits.class)
                .returnResult()
                .getResponseBody();

        assertEquals(LocalDate.now(), tidbits.getFeedDate());
        assertEquals(4, tidbits.getTriviaList().size());
        assertEquals(1, tidbits.getTriviaList().stream()
                .filter(trivia -> trivia.getTerm().equalsIgnoreCase("thermodynamics")
                        && trivia.getCategory().equals(Category.science)).count());

        assertEquals("cricket", captor.getValue()
                .getTriviaList().stream()
                .filter(trivia -> trivia.getCategory().getValue().equalsIgnoreCase(Category.sport.getValue()))
                .findAny().get().getTerm());
    }

}