package org.storck.kafkamessagingexample.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.storck.kafkamessagingexample.model.SimpleQuery;
import org.storck.kafkamessagingexample.model.SimpleResponse;

import java.util.Map;
import java.util.Properties;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    public static final String QUERY_TOPIC_NAME = "query-topic";

    public static final String RESULT_TOPIC_NAME = "result-topic";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String kafkaConsumerGroupId;

    @Bean
    public Properties streamsProperties() {
        Properties streamProperties = new Properties();
        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-messaging-example");
        streamProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        streamProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerializer.class);
        return streamProperties;
    }

    @Bean
    public ProducerFactory<String, SimpleQuery> simpleQueryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class));
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
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class));
    }

    @Bean
    public KafkaTemplate<String, SimpleResponse> simpleResponseKafkaTemplate(
            ProducerFactory<String, SimpleResponse> simpleResponseProducerFactory) {
        return new KafkaTemplate<>(simpleResponseProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, SimpleQuery> simpleQueryConsumerFactory() {
        JsonDeserializer<SimpleQuery> jsonDeserializer = new JsonDeserializer<>(SimpleQuery.class);
        jsonDeserializer.addTrustedPackages("org.storck.kafkamessagingexample.model");
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true,
                        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000,
                        ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getCanonicalName()),
                new StringDeserializer(),
                jsonDeserializer);
    }

    @Bean
    public ConsumerFactory<String, SimpleResponse> simpleResponseConsumerFactory() {
        JsonDeserializer<SimpleResponse> jsonDeserializer = new JsonDeserializer<>(SimpleResponse.class);
        jsonDeserializer.addTrustedPackages("org.storck.kafkamessagingexample.model");
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true,
                        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000,
                        ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getCanonicalName()),
                new StringDeserializer(),
                jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SimpleQuery> simpleQueryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SimpleQuery> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(simpleQueryConsumerFactory());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SimpleResponse> simpleResponseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SimpleResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(simpleResponseConsumerFactory());
        return factory;
    }

    @Bean
    public NewTopic queryTopic() {
        return new NewTopic(QUERY_TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    public NewTopic resultTopic() {
        return new NewTopic(RESULT_TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }
}
