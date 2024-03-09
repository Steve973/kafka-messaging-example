package org.storck.kafkamessagingexample.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Service;
import org.storck.kafkamessagingexample.model.SimpleResponse;

import java.io.IOException;

@Service
public class SimpleResponseSerde implements Serde<SimpleResponse> {

    private final JsonMapper jsonMapper;

    public SimpleResponseSerde(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Serializer<SimpleResponse> serializer() {
        return (topic, data) -> {
            try {
                return jsonMapper.writeValueAsBytes(data);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Override
    public Deserializer<SimpleResponse> deserializer() {
        return (topic, data) -> {
            try {
                return jsonMapper.readValue(data, SimpleResponse.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }
}