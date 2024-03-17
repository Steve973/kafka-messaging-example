package org.storck.kafkamessagingexample.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.storck.kafkamessagingexample.auth.ExternalAuthorizationService;
import org.storck.kafkamessagingexample.auth.UserDetailsServiceImpl;

import java.security.cert.X509Certificate;

@Configuration
public class SecurityConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper(new JsonFactory());
    }

    @Bean
    public RestTemplate restTemplate(SslBundles sslBundles) {
        return new RestTemplateBuilder()
                .setSslBundle(sslBundles.getBundle("server-ssl-bundle"))
                .build();
    }

    @Bean
    public ExternalAuthorizationService externalAuthorizationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new ExternalAuthorizationService(restTemplate, objectMapper);
    }

    @Bean
    public UserDetailsService userDetailsService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new UserDetailsServiceImpl(externalAuthorizationService(restTemplate, objectMapper));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RestTemplate restTemplate, ObjectMapper objectMapper) throws Exception {
        http
                .x509(httpSecurityX509Configurer ->
                        httpSecurityX509Configurer.x509PrincipalExtractor(X509Certificate::getSubjectX500Principal)
                                .userDetailsService(userDetailsService(restTemplate, objectMapper)))
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry.anyRequest().authenticated());

        return http.build();
    }
}