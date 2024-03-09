package org.storck.kafkamessagingexample.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.storck.kafkamessagingexample.model.SimpleQuery;
import org.storck.kafkamessagingexample.model.SimpleResponse;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    private Properties streamProperties;

    /**
     * Creates a new instance of QueryService.
     *
     * @param simpleQueryKafkaTemplate        the KafkaTemplate for sending SimpleQuery messages
     * @param simpleResponseKafkaTemplate     the KafkaTemplate for sending SimpleResponse messages
     * @param simpleResponseSerde             the serde for serializing/deserializing SimpleResponse objects
     * @param streamsBuilder                  the StreamsBuilder for building Kafka Streams topology
     */
    public QueryService(KafkaTemplate<String, SimpleQuery> simpleQueryKafkaTemplate,
                        KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate,
                        SimpleResponseSerde simpleResponseSerde,
                        StreamsBuilder streamsBuilder) {
        this.simpleQueryKafkaTemplate = simpleQueryKafkaTemplate;
        this.simpleResponseKafkaTemplate = simpleResponseKafkaTemplate;
        this.simpleResponseSerde = simpleResponseSerde;
        this.streamsBuilder = streamsBuilder;
    }

    /**
     * This method initializes the stream properties by setting default key and value serde classes.
     */
    @PostConstruct
    public void init() {
        this.streamProperties = new Properties();
        streamProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SimpleResponseSerde.class);
    }

    /**
     * Creates a KStream that reads from the input topic associated with the given queryId.
     *
     * @param queryId The id of the query.
     * @return The KStream object representing the input topic stream.
     */
    public KStream<String, SimpleResponse> createInputTopicStream(String queryId) {
        return streamsBuilder.stream("query-response-" + queryId, Consumed.with(Serdes.String(), simpleResponseSerde));
    }

    /**
     * Filters the input stream by queryId and returns a new stream containing only the matching records.
     *
     * @param queryId The queryId to filter the stream by.
     * @param inputTopicStream The input stream to be filtered.
     * @return A new stream containing only the records matching the given queryId.
     */
    public KStream<String, SimpleResponse> filterStreamByQueryId(String queryId, KStream<String, SimpleResponse> inputTopicStream) {
        return inputTopicStream.filter((key, value) -> value.getId().equals(queryId));
    }

    /**
     * Processes a local query, sending it to a Kafka topic and collecting responses.
     *
     * @param query The query to be processed.
     * @return A list of strings representing the combined result of processing the query and collecting responses.
     * @throws ExecutionException If an exception occurs during the execution of the method.
     * @throws InterruptedException If the thread is interrupted while waiting for the result.
     */
    public List<String> processLocalQuery(String query) throws ExecutionException, InterruptedException {
        SimpleQuery simpleQuery = SimpleQuery.builder()
                .id(UUID.randomUUID().toString())
                .query(query)
                .build();
        return simpleQueryKafkaTemplate.send("query-topic", simpleQuery)
                .thenCompose(sendResult -> {
                    CompletableFuture<List<String>> processFuture = CompletableFuture.supplyAsync(
                            () -> processQuery(sendResult.getProducerRecord().value().getQuery()));
                    CompletableFuture<List<String>> responsesFuture = CompletableFuture.supplyAsync(
                            () -> collectResponses(sendResult.getProducerRecord().value().getId(),
                                    Duration.of(30, ChronoUnit.SECONDS)));
                    return processFuture.thenCombine(responsesFuture, (processList, responseList) -> {
                        List<String> combined = new ArrayList<>();
                        combined.addAll(processList);
                        combined.addAll(responseList);
                        return combined;
                    });
                })
                .exceptionally(ex -> {
                    // Handle the exception.
                    throw new IllegalStateException("Sending failed", ex);
                })
                .get();
    }

    /**
     * Listens for queries and processes them.
     *
     * @param simpleQuery the SimpleQuery object representing the received query
     */
    @KafkaListener(topics = "query-topic", groupId = "group_id")
    public void listenForQueries(SimpleQuery simpleQuery) {
        SimpleResponse response = SimpleResponse.builder()
                .id(simpleQuery.getId())
                .results(processQuery(simpleQuery.getQuery()))
                .build();
        simpleResponseKafkaTemplate.send("result-topic", response);
    }

    /**
     * Processes the given query and returns a list of formatted system properties.
     *
     * @param query the query to process
     * @return a list of formatted system properties
     */
    private List<String> processQuery(String query) {
        log.info("Received query: {}", query);
        return List.of("""
                OS Name: %s
                OS Version: %s
                OS Architecture: %s
                User Name: %s
                User Home: %s
                """.formatted(System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), System.getProperty("user.name"), System.getProperty("user.home")));
    }

    /**
     * Collects responses from a Kafka stream for a given query ID within a specified timeout duration.
     *
     * @param queryId  the ID of the query
     * @param timeout  the timeout duration to collect responses
     * @return a list of collected responses
     */
    public List<String> collectResponses(String queryId, Duration timeout) {
        long startTime = System.currentTimeMillis();
        List<String> responses = new ArrayList<>();
        KStream<String, SimpleResponse> inputStream = createInputTopicStream(queryId);
        KStream<String, SimpleResponse> filteredStream = filterStreamByQueryId(queryId, inputStream);
        CountDownLatch latch = new CountDownLatch(1);
        filteredStream.peek((key, value) -> {
            responses.addAll(value.getResults());
            if (System.currentTimeMillis() - startTime >= timeout.toMillis()) {
                latch.countDown();
            }
        });
        try (KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), streamProperties)) {
            streams.start();
            boolean latchResult = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            log.info("Processing of query with id '{}' complete by duration timeout: {}", queryId, latchResult);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting interrupted", e);
        }
        return responses;
    }
}