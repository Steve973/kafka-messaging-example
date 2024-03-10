package org.storck.kafkamessagingexample.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.storck.kafkamessagingexample.model.SimpleQuery;
import org.storck.kafkamessagingexample.model.SimpleResponse;
import org.storck.kafkamessagingexample.service.SimpleResponseSerde;

import java.util.Map;
import java.util.Properties;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    private final String bootstrapAddress;

    public KafkaConfiguration(@Value(value = "${spring.kafka.bootstrap-servers}") String bootstrapAddress) {
        this.bootstrapAddress = bootstrapAddress;
        System.err.println("########## bootstrap address: " + bootstrapAddress);
    }

    @Bean
    public Properties streamProperties() {
        Properties streamProperties = new Properties();
        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-messaging-example");
        streamProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        streamProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerializer.class);
        return streamProperties;
    }

    @Bean
    public ProducerFactory<String, SimpleQuery> simpleQueryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class),
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, SimpleQuery> simpleQueryKafkaTemplate(
            ProducerFactory<String, SimpleQuery> simpleQueryProducerFactory) {
        return new KafkaTemplate<>(simpleQueryProducerFactory);
    }

    @Bean
    public ProducerFactory<String, SimpleResponse> simpleResponseProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class),
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate(
            ProducerFactory<String, SimpleResponse> simpleResponseProducerFactory) {
        return new KafkaTemplate<>(simpleResponseProducerFactory);
    }

    @Bean
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }
}
