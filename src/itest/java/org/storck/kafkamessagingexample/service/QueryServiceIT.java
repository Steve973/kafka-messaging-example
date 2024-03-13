package org.storck.kafkamessagingexample.service;

import org.apache.kafka.streams.StreamsBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.storck.kafkamessagingexample.config.KafkaConfiguration;
import org.storck.kafkamessagingexample.model.SimpleQuery;
import org.storck.kafkamessagingexample.model.SimpleResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueryServiceIT {

    static final String REDPANDA_TAG = "redpandadata/redpanda:v23.3.6";

    @Container
    static RedpandaContainer redpandaContainer =
            new RedpandaContainer(DockerImageName.parse(REDPANDA_TAG));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpandaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", redpandaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", redpandaContainer::getBootstrapServers);
        registry.add("spring.kafka.streams.bootstrap-servers", redpandaContainer::getBootstrapServers);
    }

    @Autowired
    QueryService queryService1;

    @Autowired
    QueryService queryService2;

    @Test
    void myTest() throws Exception {
        List<String> results = queryService1.processLocalQuery("test_query", Duration.ofSeconds(5));
        assertNotNull(results);
        System.err.println("Results: \n" + String.join(", \n", results));
    }

    @TestConfiguration
    @Import({KafkaConfiguration.class})
    static class TestConfig {

        @Autowired
        private KafkaTemplate<String, SimpleQuery> simpleQueryKafkaTemplate;

        @Autowired
        private KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate;

        @Autowired
        private SimpleResponseSerde simpleResponseSerde;

        @Autowired
        private StreamsBuilder streamsBuilder;

        @Autowired
        private Properties streamsProperties;

        @Bean
        public QueryService queryService1() {
            return new QueryService(simpleQueryKafkaTemplate, simpleResponseKafkaTemplate, simpleResponseSerde, streamsBuilder, streamsProperties);
        }

        @Bean
        public QueryService queryService2() {
            return new QueryService(simpleQueryKafkaTemplate, simpleResponseKafkaTemplate, simpleResponseSerde, streamsBuilder, streamsProperties);
        }
    }
}
