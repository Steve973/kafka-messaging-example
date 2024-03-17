package org.storck.kafkamessagingexample.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.storck.kafkamessagingexample.model.SimpleQuery;
import org.storck.kafkamessagingexample.model.SimpleResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.storck.kafkamessagingexample.config.KafkaConfiguration.QUERY_TOPIC_NAME;
import static org.storck.kafkamessagingexample.config.KafkaConfiguration.RESULT_TOPIC_NAME;

/**
 * Handles querying and processing of {@link SimpleQuery}s and {@link SimpleResponse}s using Kafka.
 */
@Slf4j
@Service
public class QueryService {

    private final KafkaTemplate<String, SimpleQuery> simpleQueryKafkaTemplate;

    private final KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate;

    private final SimpleResponseSerde simpleResponseSerde;

    private final StreamsBuilder streamsBuilder;

    private final Properties streamsProperties;

    private final Cache<String, String> queryIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    public QueryService(KafkaTemplate<String, SimpleQuery> simpleQueryKafkaTemplate,
                        KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate,
                        SimpleResponseSerde simpleResponseSerde,
                        StreamsBuilder streamsBuilder,
                        Properties streamsProperties) {
        this.simpleQueryKafkaTemplate = simpleQueryKafkaTemplate;
        this.simpleResponseKafkaTemplate = simpleResponseKafkaTemplate;
        this.simpleResponseSerde = simpleResponseSerde;
        this.streamsBuilder = streamsBuilder;
        this.streamsProperties = streamsProperties;
    }

    public List<String> processLocalQuery(String query, Duration timeout)
            throws CompletionException, ExecutionException, InterruptedException {
        String queryId = UUID.randomUUID().toString();
        SimpleQuery simpleQuery = SimpleQuery.builder()
                .id(queryId)
                .query(query + " (broadcast)")
                .build();
        queryIdCache.put(queryId, query);

        CompletableFuture<SendResult<String, SimpleQuery>> sendFuture = simpleQueryKafkaTemplate.send("query-topic", simpleQuery);

        CompletableFuture<List<String>> localProcessingFuture = sendFuture.thenApplyAsync(sendResult -> {
            try {
                return processQuery(query);
            } catch (Exception e) {
                throw new CompletionException("Error processing query", e);
            }
        });

        CompletableFuture<List<String>> remoteProcessingFuture = sendFuture.thenCompose(sendResult ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return collectResponses(simpleQuery.getId(), timeout);
                    } catch (Exception e) {
                        throw new CompletionException("Error collecting responses", e);
                    }
                }));

        return localProcessingFuture.thenCombineAsync(remoteProcessingFuture, (localResponse, remoteResponses) -> {
            List<String> combined = new ArrayList<>();
            combined.addAll(localResponse);
            combined.addAll(remoteResponses);
            return combined;
        }).exceptionally(ex -> {
            throw new IllegalStateException("Failed to process local query", ex);
        }).get();
    }

    private List<String> collectResponses(String queryId, Duration timeout) throws InterruptedException {
        List<String> responses = new ArrayList<>();
        streamsBuilder
                .stream(RESULT_TOPIC_NAME, Consumed.with(Serdes.String(), simpleResponseSerde))
                .filter((key, value) -> value.getId().equals(queryId))
                .foreach((key, value) -> responses.addAll(value.getResults()));
        CountDownLatch latch = new CountDownLatch(1);
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(LogAndFailExceptionHandler.class.getClassLoader());
        try (KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), streamsProperties)) {
            streams.start();
            latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassloader);
        }
        return responses;
    }

    @KafkaListener(topics = QUERY_TOPIC_NAME,
            groupId = "query_consumer_ + #{T(java.util.UUID).randomUUID().toString()}",
            containerFactory = "simpleQueryKafkaListenerContainerFactory",
            autoStartup = "true")
    public void listenForQueries(SimpleQuery simpleQuery) {
        if (queryIdCache.getIfPresent(simpleQuery.getId()) == null) {
            SimpleResponse response = SimpleResponse.builder()
                    .id(simpleQuery.getId())
                    .results(processQuery(simpleQuery.getQuery()))
                    .build();
            simpleResponseKafkaTemplate.send(RESULT_TOPIC_NAME, "result", response);
        }
    }

    private List<String> processQuery(String query) {
        log.info("Received query: {}", query);
        return List.of(
                "=======================================================",
                "query: " + query,
                "OS Name: " + System.getProperty("os.name"),
                "OS Version: " + System.getProperty("os.version"),
                "OS Architecture: " + System.getProperty("os.arch"),
                "User Name: " + System.getProperty("user.name"),
                "User Home: " + System.getProperty("user.home"),
                "======================================================="
        );
    }
}
