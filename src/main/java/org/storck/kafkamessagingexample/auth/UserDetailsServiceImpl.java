package org.storck.kafkamessagingexample.auth;

import lombok.extern.slf4j.Slf4j;
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
        return externalAuthorizationService.getUserByUsername(username);
    }

    public UserDetails loadUserByCertInfo(String userDn, String issuerDn) throws UsernameNotFoundException {
        log.warn("########## User Details Service -- loading user by userDn: {} and issuerDn: {}", userDn, issuerDn);
        return externalAuthorizationService.getUserByCertInfo(userDn, issuerDn);
    }
}