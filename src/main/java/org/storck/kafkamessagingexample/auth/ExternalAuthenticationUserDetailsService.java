package org.storck.kafkamessagingexample.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class ExternalAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private final String authServiceUrl = "https://your-auth-service.com/api/user";

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    public ExternalAuthenticationUserDetailsService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(cacheNames = "userDetails", key = "#token.getPrincipal")
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        String principal = (String) token.getPrincipal();
        String[] parts = principal.split("::");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Invalid principal format");
        }
        String subject = parts[0];
        String issuer = parts[1];
        log.warn("########## Authenticating user with Subject DN: {} and Issuer DN: {}", subject, issuer);
        String[] auths = new String[] {"Auth1", "Auth2", "Auth3"};
        UserDetails userDetails = User.builder()
                .password("not_used")
                .username(subject)
                .authorities(auths)
                .build();
        log.warn("########## User authorities: {}", userDetails.getAuthorities());
        return userDetails;
    }
}