package org.storck.kafkamessagingexample.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class ExternalAuthorizationService {

    private final String authServiceUrl = "https://your-auth-service.com/api/user";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalAuthorizationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = "userCredentials", key = "#dn")
    public UserCredentials getUserCredentials(String dn) {
        log.warn("########## Authenticating user with DN: {}", dn);
        return new UserCredentials(dn, Stream.of("Auth1", "Auth2", "Auth3")
                .map(SimpleGrantedAuthority::new)
                .toList());
    }

//    public UserCredentials getUserCredentials(String dn) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//        HttpEntity<String> request = new HttpEntity<>("{\"dn\":\"" + dn + "\"}", headers);
//
//        ResponseEntity<String> response = restTemplate.exchange(authServiceUrl, HttpMethod.POST, request, String.class);
//
//        if (response.getStatusCode().is2xxSuccessful()) {
//            try {
//                JsonNode root = objectMapper.readTree(response.getBody());
//                String username = root.path("username").asText();
//                String[] roles = objectMapper.convertValue(root.path("roles"), String[].class);
//
//                return new UserCredentials(username, Arrays.stream(roles)
//                        .map(SimpleGrantedAuthority::new)
//                        .toList());
//            } catch (Exception e) {
//                throw new IllegalArgumentException("Failed to parse response from external authorization service", e);
//            }
//        } else {
//            throw new IllegalStateException("External authorization service returned error response: " + response.getStatusCode());
//        }
//    }
}