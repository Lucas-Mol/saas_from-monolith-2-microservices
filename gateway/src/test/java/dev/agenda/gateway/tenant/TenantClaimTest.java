package dev.agenda.gateway.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TenantClaimTest {

    @Test
    void readsKeycloakOrganizationListClaim() {
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("tenant_id", List.of("acme-clinic"))))).containsExactly("acme-clinic");
    }

    @Test
    void readsSingleStringClaim() {
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("tenant_id", "bella-salon")))).containsExactly("bella-salon");
    }

    @Test
    void returnsEveryTenantSoCallersCanRejectAmbiguity() {
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("tenant_id", List.of("acme-clinic", "bella-salon")))))
                .containsExactly("acme-clinic", "bella-salon");
    }

    @Test
    void returnsNothingWhenTheClaimIsAbsentOrNotText() {
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("email", "ops@agenda.test")))).isEmpty();
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("tenant_id", 42)))).isEmpty();
        assertThat(TenantClaim.tenantsOf(jwt(Map.of("tenant_id", List.of(42))))).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token").header("alg", "RS256").claims(c -> c.putAll(claims))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
