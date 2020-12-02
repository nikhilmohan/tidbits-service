package com.nikhilm.hourglass.tidbitsservice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@SpringBootApplication
public class TidbitsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TidbitsServiceApplication.class, args);
	}

	@Bean
	ReactiveCircuitBreakerFactory circuitBreakerFactory()	{
		var factory = new ReactiveResilience4JCircuitBreakerFactory();
		factory.configureDefault(s -> new Resilience4JConfigBuilder(s)
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5L)).build())
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				.build());
		return factory;
	}
	@Bean
	WebClient webClient()	{
		return WebClient.create();
	}

}
