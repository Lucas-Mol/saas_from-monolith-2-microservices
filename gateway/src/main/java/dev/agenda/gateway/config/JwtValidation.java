package dev.agenda.gateway.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

public final class JwtValidation {

    public static final String AUDIENCE = "gateway";

    private JwtValidation() {
    }

    public static OAuth2TokenValidator<Jwt> validators(String issuer) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        audience -> audience != null && audience.contains(AUDIENCE)),
                JwtValidation::notNarrowedToOneOrganization);
    }

    static OAuth2TokenValidatorResult notNarrowedToOneOrganization(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        boolean narrowed = scope != null && Arrays.stream(scope.split(" ")).anyMatch(s -> s.startsWith("organization:"));
        return narrowed
                ? OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Organization-specific scopes are not accepted", null))
                : OAuth2TokenValidatorResult.success();
    }

    public static List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}
