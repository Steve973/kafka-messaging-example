package org.storck.kafkamessagingexample.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }
}