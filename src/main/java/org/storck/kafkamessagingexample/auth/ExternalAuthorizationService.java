package org.storck.kafkamessagingexample.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Cacheable(cacheNames = "userDetails", key = "#subjectDn")
    public UserDetails getUserByUsername(String subjectDn) {
        log.warn("########## Authenticating user with Subject DN: {}", subjectDn);
        String[] auths = new String[] {"Auth1", "Auth2", "Auth3"};
        UserDetails userDetails = User.builder()
                .password("not_used")
                .username(subjectDn)
                .authorities(auths)
                .build();
        log.warn("########## User authorities: {}", userDetails.getAuthorities());
        return userDetails;
    }

    @Cacheable(cacheNames = "userDetails", key = "#subjectDn + '::' + #issuerDn")
    public UserDetails getUserByCertInfo(String subjectDn, String issuerDn) {
        log.warn("########## Authenticating user with Subject DN: {} and Issuer DN: {}", subjectDn, issuerDn);
        String[] auths = new String[] {"Auth1", "Auth2", "Auth3"};
        UserDetails userDetails = User.builder()
                .password("not_used")
                .username(subjectDn)
                .authorities(auths)
                .build();
        log.warn("########## User authorities: {}", userDetails.getAuthorities());
        return userDetails;
    }
}