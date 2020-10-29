package com.nikhilm.hourglass.tidbitsservice.repositories;

import com.nikhilm.hourglass.tidbitsservice.models.Category;
import com.nikhilm.hourglass.tidbitsservice.models.TidbitFeed;
import com.nikhilm.hourglass.tidbitsservice.models.Topic;
import org.springframework.cglib.core.Local;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TidbitFeedRepository extends ReactiveMongoRepository<TidbitFeed, String> {

    Mono<TidbitFeed> findByFeedDate(LocalDate now);

}
