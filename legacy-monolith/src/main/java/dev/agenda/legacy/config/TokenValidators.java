package dev.agenda.legacy.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import dev.agenda.legacy.users.TokenClaims;

public final class TokenValidators {

    public static final String API_AUDIENCE = "gateway";

    private TokenValidators() {
    }

    public static OAuth2TokenValidator<Jwt> accessToken(String issuer, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("legacy.tenant-id must be configured");
        }
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        audience -> audience != null && audience.contains(API_AUDIENCE)),
                jwt -> TokenClaims.tenants(jwt.getClaims()).equals(List.of(tenantId))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Token does not belong to this tenant", null)),
                jwt -> TokenClaims.narrowedToOneOrganization(TokenClaims.scopes(jwt.getClaimAsString("scope")))
                        ? OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Organization-specific scopes are not accepted", null))
                        : OAuth2TokenValidatorResult.success());
    }
}
