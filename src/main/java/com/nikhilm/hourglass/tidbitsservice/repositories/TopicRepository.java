package com.nikhilm.hourglass.tidbitsservice.repositories;

import com.nikhilm.hourglass.tidbitsservice.models.Category;
import com.nikhilm.hourglass.tidbitsservice.models.Tidbits;
import com.nikhilm.hourglass.tidbitsservice.models.Topic;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TopicRepository extends ReactiveMongoRepository<Topic, String> {

    @Query(value = "{$group : {category : $category}", count = true)
    Mono<Long> findCountByCategory();

    @Query(value = "{category : ?0}" )
    Flux<Topic> findByCategory(Category category);


}
