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
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CorsFilter;
import org.storck.kafkamessagingexample.auth.ClientCertAuthFilter;
import org.storck.kafkamessagingexample.auth.ExternalAuthenticationUserDetailsService;

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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService(
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new ExternalAuthenticationUserDetailsService(restTemplate, objectMapper);
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(
            AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService) {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public ClientCertAuthFilter clientCertAuthFilter(AuthenticationManager authenticationManager) {
        ClientCertAuthFilter filter = new ClientCertAuthFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ClientCertAuthFilter clientCertAuthFilter, PreAuthenticatedAuthenticationProvider preAuthProvider)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(clientCertAuthFilter, CorsFilter.class)
                .authenticationProvider(preAuthProvider)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health")).permitAll()
                                .anyRequest().authenticated())
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}