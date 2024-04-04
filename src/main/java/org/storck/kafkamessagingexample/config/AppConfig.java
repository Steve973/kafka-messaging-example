package org.storck.kafkamessagingexample.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(servers = {@Server(url = "/", description = "Default Server URL")})
public class AppConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }
}