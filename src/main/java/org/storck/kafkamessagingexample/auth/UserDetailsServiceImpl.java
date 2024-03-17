package org.storck.kafkamessagingexample.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ExternalAuthorizationService externalAuthorizationService;

    public UserDetailsServiceImpl(ExternalAuthorizationService externalAuthorizationService) {
        this.externalAuthorizationService = externalAuthorizationService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.warn("########## User Details Service -- loading user by username: {}", username);
        // Extract DN from the provided username (client certificate)
        String dn = extractDNFromUsername(username);

        // Call external authorization service to get user credentials based on DN
        UserCredentials userCredentials = externalAuthorizationService.getUserCredentials(dn);

        // Create and return UserDetails object based on the retrieved user credentials
        return new User(userCredentials.getUsername(), "", userCredentials.getAuthorities());
    }

    // Helper method to extract DN from the provided username
    private String extractDNFromUsername(String username) {
        // Implement your logic to extract the DN from the username
        // This will depend on how the username is formatted in your case
        return username;
    }
}