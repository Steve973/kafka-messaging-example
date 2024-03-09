package org.storck.kafkamessagingexample.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
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

import java.util.Map;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    private final String bootstrapAddress;

    public KafkaConfiguration(@Value(value = "${kafka.bootstrapAddress}") String bootstrapAddress) {
        this.bootstrapAddress = bootstrapAddress;
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
