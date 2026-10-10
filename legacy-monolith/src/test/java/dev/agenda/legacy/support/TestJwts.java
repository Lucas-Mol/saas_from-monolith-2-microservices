package dev.agenda.legacy.support;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import dev.agenda.legacy.config.TokenValidators;

public final class TestJwts {

    public static final String ISSUER = "http://issuer.test/realms/agenda";
    public static final String TENANT = "test-tenant";

    private static final RSAKey KEY = generateKey();
    private static final Map<String, String> ROLES_BY_USER = Map.of(
            "admin@test.local", "ADMIN",
            "staff@test.local", "STAFF",
            "ana.user@test.local", "CUSTOMER",
            "orphan@test.local", "CUSTOMER");

    private TestJwts() {
    }

    public static String forUser(String email) {
        String role = ROLES_BY_USER.get(email);
        if (role == null) {
            throw new IllegalArgumentException("Unknown test user " + email);
        }
        return token().email(email).roles(role).build();
    }

    public static Builder token() {
        return new Builder();
    }

    public static String jwks() {
        return new JWKSet(KEY.toPublicJWK()).toString();
    }

    public static String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(KEY));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static JwtDecoder decoder() {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(TokenValidators.accessToken(ISSUER, TENANT));
            return decoder;
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static final class Builder {

        private final Map<String, Object> claims = new HashMap<>();
        private String issuer = ISSUER;
        private List<String> audience = List.of(TokenValidators.API_AUDIENCE);
        private Instant expiresAt = Instant.now().plusSeconds(300);
        private RSAKey signingKey = KEY;

        private Builder() {
            claims.put("tenant_id", List.of(TENANT));
            claims.put("email", "someone@test.local");
            claims.put("email_verified", true);
        }

        public Builder emailVerified(boolean verified) {
            claims.put("email_verified", verified);
            return this;
        }

        public Builder scope(String scope) {
            claims.put("scope", scope);
            return this;
        }

        public Builder email(String email) {
            claims.put("email", email);
            claims.put("name", email);
            return this;
        }

        public Builder roles(String... roles) {
            claims.put("realm_access", Map.of("roles", List.of(roles)));
            return this;
        }

        public Builder tenant(Object tenantClaim) {
            if (tenantClaim == null) {
                claims.remove("tenant_id");
            } else {
                claims.put("tenant_id", tenantClaim);
            }
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder audience(String... audience) {
            this.audience = List.of(audience);
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder signedWithUnknownKey() {
            this.signingKey = generateKey();
            return this;
        }

        public String build() {
            JWTClaimsSet.Builder set = new JWTClaimsSet.Builder()
                    .subject(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(expiresAt));
            claims.forEach(set::claim);
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                        set.build());
                jwt.sign(new RSASSASigner(signingKey));
                return jwt.serialize();
            } catch (JOSEException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
