package org.storck.kafkamessagingexample.auth;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class ClientCertificateAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private final String clientCertPem;

    public ClientCertificateAuthenticationToken(String clientCertPem) {
        super(null);
        this.principal = null;
        this.clientCertPem = clientCertPem;
        setAuthenticated(false);
    }

    public ClientCertificateAuthenticationToken(UserDetails userDetails, String clientCertPem, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = userDetails;
        this.clientCertPem = clientCertPem;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}