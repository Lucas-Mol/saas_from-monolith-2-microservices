package dev.agenda.gateway.support;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import dev.agenda.gateway.config.JwtValidation;

public final class TestJwts {

    public static final String ISSUER = "http://issuer.test/realms/agenda";

    private static final RSAKey KEY = generateKey();

    private TestJwts() {
    }

    public static Builder token() {
        return new Builder();
    }

    public static String forTenant(String tenantId, String... roles) {
        return token().tenant(tenantId).roles(roles).build();
    }

    public static ReactiveJwtDecoder decoder() {
        try {
            NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(JwtValidation.validators(ISSUER));
            return decoder;
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static RSAPublicKey publicKey() {
        try {
            return KEY.toRSAPublicKey();
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
        private List<String> audience = List.of(JwtValidation.AUDIENCE);
        private Instant expiresAt = Instant.now().plusSeconds(300);
        private RSAKey signingKey = KEY;

        private Builder() {
            claims.put("email", "user@test.local");
        }

        public Builder tenant(Object tenantClaim) {
            claims.put("tenant_id", tenantClaim instanceof String single ? List.of(single) : tenantClaim);
            return this;
        }

        public Builder roles(String... roles) {
            claims.put("realm_access", Map.of("roles", List.of(roles)));
            return this;
        }

        public Builder scope(String scope) {
            claims.put("scope", scope);
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
                    .audience(new ArrayList<>(audience))
                    .issueTime(java.util.Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(java.util.Date.from(expiresAt));
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
