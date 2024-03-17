package org.storck.kafkamessagingexample.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Cacheable(cacheNames = "userDetails", key = "#dn")
    public UserDetails getUserCredentials(String dn) {
        log.warn("########## Authenticating user with DN: {}", dn);
        String[] auths = new String[] {"Auth1", "Auth2", "Auth3"};
        UserDetails userDetails = User.builder()
                .password("not_used")
                .username(dn)
                .authorities(auths)
                .build();
        log.warn("########## User authorities: {}", userDetails.getAuthorities());
        return userDetails;
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