package org.storck.kafkamessagingexample.service;

import com.github.dockerjava.api.model.Capability;
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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueryServiceIT {

    private static final int REDPANDA_PORT = 9092;

    private static final int REDPANDA_ADMIN_PORT = 9644;

    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private static final int REST_PROXY_PORT = 8082;

    static final String REDPANDA_TAG = "docker.redpanda.com/redpandadata/redpanda:v23.3.6";

    @Container
    static RedpandaContainer redpandaContainer =
            new RedpandaContainer(DockerImageName.parse(REDPANDA_TAG))
                    .waitingFor(new HostPortWaitStrategy())
                    .waitingFor(Wait.forLogMessage(".*Successfully started Redpanda!.*", 1))
                    .withExposedPorts(REDPANDA_PORT, REDPANDA_ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT)
                    .withCreateContainerCmdModifier(cmd -> {
                        cmd.withEntrypoint("/entrypoint-tc.sh");
                        cmd.withUser("root:root");
                    })
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("testcontainers/entrypoint-tc.sh", 744),
                            "/entrypoint-tc.sh")
                    .withCommand("redpanda", "start", "--mode=dev-container", "--smp=1", "--memory=1G");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpandaContainer::getBootstrapServers);
//        registry.add("spring.kafka.consumer.bootstrap-servers", redpandaContainer::getBootstrapServers);
//        registry.add("spring.kafka.producer.bootstrap-servers", redpandaContainer::getBootstrapServers);
//        registry.add("spring.kafka.streams.bootstrap-servers", redpandaContainer::getBootstrapServers);
    }

    @Autowired
    QueryService queryService1;

    @Autowired
    QueryService queryService2;

    @Test
    void myTest() throws ExecutionException, InterruptedException {
        List<String> results = queryService1.processLocalQuery("test_query");
        assertNotNull(results);
        System.err.println("Results: " + String.join(", \n", results));
    }

    @TestConfiguration
    @Import(KafkaConfiguration.class)
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
        private Properties streamProperties;

        @Bean
        public QueryService queryService1() {
            return new QueryService(simpleQueryKafkaTemplate, simpleResponseKafkaTemplate, simpleResponseSerde, streamsBuilder, streamProperties);
        }

        @Bean
        public QueryService queryService2() {
            return new QueryService(simpleQueryKafkaTemplate, simpleResponseKafkaTemplate, simpleResponseSerde, streamsBuilder, streamProperties);
        }
    }
}
