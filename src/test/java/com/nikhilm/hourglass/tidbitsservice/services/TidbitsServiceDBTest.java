package com.nikhilm.hourglass.tidbitsservice.services;

import com.nikhilm.hourglass.tidbitsservice.models.Category;
import com.nikhilm.hourglass.tidbitsservice.models.Topic;
import com.nikhilm.hourglass.tidbitsservice.models.TopicStats;
import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@ExtendWith(SpringExtension.class)
@DataMongoTest
@Slf4j
public class TidbitsServiceDBTest {

    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @Autowired
    TopicRepository topicRepository;

    @BeforeEach
    public void setup() {
        Topic topic1 = new Topic("physics", Category.science);
        Topic topic2 = new Topic("plitvice", Category.travel);
        Topic topic3 = new Topic("football", Category.sport);
        Topic topic4 = new Topic("blockchain", Category.technology);

        List<Topic> topics = new ArrayList<>();

        topicRepository.deleteAll()
                .thenMany(Flux.fromIterable(topics))
                .flatMap(topicRepository::save)
                .blockLast();


        topicRepository.saveAll(Flux.fromIterable(Arrays.asList(topic1, topic2, topic3, topic4))).blockLast();
        log.info("Saved topics! ");



    }

    @Test
    public void testGroupByCategory()   {

        List<TopicStats> topicStatsFlux = Arrays.asList(new TopicStats(Category.science, 1),
                new TopicStats(Category.technology, 1), new TopicStats(Category.sport, 1),
                new TopicStats(Category.travel, 1));

        GroupOperation groupByCategoryAndCount = group("category")
                .count().as("count");

        Aggregation aggregation = newAggregation(groupByCategoryAndCount);

        StepVerifier.create(mongoTemplate.aggregate(aggregation, Topic.class, TopicStats.class))
                .expectSubscription()
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .expectNextMatches(topicStats -> topicStats.getCount() == 1)
                .verifyComplete();
    }

}
