package dev.agenda.legacy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "legacy.oidc")
public record OidcProperties(String issuerUri, String clientId, String clientSecret) {
}
