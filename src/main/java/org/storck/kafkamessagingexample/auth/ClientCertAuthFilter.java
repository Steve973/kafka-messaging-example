package org.storck.kafkamessagingexample.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
@NoArgsConstructor
public class ClientCertAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        log.info("########## ClientCertAuthFilter: getting pre-authenticated principal");
        String clientCertPem = request.getHeader("X-Forwarded-Tls-Client-Cert");
        if (clientCertPem != null) {
            byte[] decoded = Base64.getDecoder().decode(clientCertPem);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
                String subject = cert.getSubjectX500Principal().getName();
                String issuer = cert.getIssuerX500Principal().getName();
                log.info("########## Got principal: {}::{}", subject, issuer);
                return subject + "::" + issuer;
            } catch (CertificateException e) {
                throw new IllegalArgumentException(
                        "Error while parsing client certificate from request for subject and issuer information", e);
            }
        }
        return null;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "NOT_APPLICABLE";
    }
}