package org.storck.kafkamessagingexample.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Component
public class ClientCertificateAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsServiceImpl userDetailsService;

    public ClientCertificateAuthenticationProvider(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof ClientCertificateAuthenticationToken authToken) {
            String clientCertPem = authToken.getClientCertPem();
            byte[] decoded = Base64.getDecoder().decode(clientCertPem);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
                String subjectDN = cert.getSubjectX500Principal().getName();
                String issuerDN = cert.getIssuerX500Principal().getName();
                UserDetails userDetails = userDetailsService.loadUserByCertInfo(subjectDN, issuerDN);
                if (userDetails != null) {
                    return new ClientCertificateAuthenticationToken(userDetails, clientCertPem, userDetails.getAuthorities());
                }
            } catch (CertificateException e) {
                throw new IllegalArgumentException(
                        "Error while parsing client certificate from request for subject and issuer information", e);
            }
        }
        throw new BadCredentialsException("Invalid client certificate");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ClientCertificateAuthenticationToken.class.isAssignableFrom(authentication);
    }
}