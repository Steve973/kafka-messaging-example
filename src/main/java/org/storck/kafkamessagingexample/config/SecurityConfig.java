package org.storck.kafkamessagingexample.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.storck.kafkamessagingexample.auth.ClientCertificateAuthenticationFilter;
import org.storck.kafkamessagingexample.auth.ClientCertificateAuthenticationProvider;
import org.storck.kafkamessagingexample.auth.ExternalAuthorizationService;
import org.storck.kafkamessagingexample.auth.UserDetailsServiceImpl;

@Configuration
@EnableMethodSecurity
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
    public UserDetailsServiceImpl userDetailsService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new UserDetailsServiceImpl(externalAuthorizationService(restTemplate, objectMapper));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, UserDetailsServiceImpl userDetailsService, AuthenticationManager authenticationManager)
            throws Exception {
        http
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry.anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(new ClientCertificateAuthenticationProvider(userDetailsService))
                .addFilterBefore(new ClientCertificateAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}