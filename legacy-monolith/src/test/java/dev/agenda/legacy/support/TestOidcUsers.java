package dev.agenda.legacy.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import dev.agenda.legacy.users.LegacyOidcUser;
import dev.agenda.legacy.users.LegacyUserPrincipal;

public final class TestOidcUsers {

    private TestOidcUsers() {
    }

    public static LegacyOidcUser admin() {
        return user("admin@test.local", "Test Admin", "ADMIN", null);
    }

    public static LegacyOidcUser staff() {
        return user("staff@test.local", "Test Staff", "STAFF", null);
    }

    public static LegacyOidcUser customer() {
        return user("ana.user@test.local", "Ana Test", "CUSTOMER", 1L);
    }

    public static OidcIdToken idToken(String email, String role, Object tenantClaim) {
        OidcIdToken.Builder builder = OidcIdToken.withTokenValue("id-token")
                .issuer(TestJwts.ISSUER)
                .subject(email)
                .audience(List.of("backoffice-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", email)
                .claim("realm_access", Map.of("roles", List.of(role)));
        if (tenantClaim != null) {
            builder.claim("tenant_id", tenantClaim);
        }
        return builder.build();
    }

    private static LegacyOidcUser user(String email, String fullName, String role, Long customerId) {
        return new LegacyOidcUser(new LegacyUserPrincipal(email, fullName, role, customerId),
                idToken(email, role, List.of(TestJwts.TENANT)), null);
    }
}
