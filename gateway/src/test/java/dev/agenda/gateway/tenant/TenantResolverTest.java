package dev.agenda.gateway.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.agenda.gateway.tenant.TenantResolution.Reason;
import dev.agenda.gateway.tenant.TenantResolution.Resolved;
import dev.agenda.gateway.tenant.TenantResolution.Unresolved;

class TenantResolverTest {

    private final TenantResolver resolver = new TenantResolver("localhost");

    @Test
    void resolvesBrowserTenantFromSubdomain() {
        assertThat(resolver.fromHost("acme-clinic.localhost:8080")).isEqualTo(new Resolved("acme-clinic"));
        assertThat(resolver.fromHost("ACME-Clinic.LocalHost.")).isEqualTo(new Resolved("acme-clinic"));
    }

    @Test
    void reportsMissingTenantForHostsWithoutTenantSubdomain() {
        assertThat(resolver.fromHost("localhost:8080")).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.fromHost(null)).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.fromHost("[::1]:8080")).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.fromHost("acme-clinic.example.com")).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.fromHost("acme-clinic-localhost")).isEqualTo(new Unresolved(Reason.MISSING));
    }

    @Test
    void rejectsMalformedSubdomains() {
        assertThat(resolver.fromHost("a.b.localhost")).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.fromHost(".localhost")).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.fromHost("-acme.localhost")).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.fromHost("a".repeat(64) + ".localhost")).isEqualTo(new Unresolved(Reason.INVALID));
    }

    @Test
    void resolvesApiTenantFromTheToken() {
        assertThat(resolver.fromToken("localhost:8080", List.of("bella-salon"))).isEqualTo(new Resolved("bella-salon"));
        assertThat(resolver.fromToken("gateway:8080", List.of("bella-salon"))).isEqualTo(new Resolved("bella-salon"));
    }

    @Test
    void acceptsTokenTenantMatchingTheSubdomain() {
        assertThat(resolver.fromToken("acme-clinic.localhost", List.of("acme-clinic")))
                .isEqualTo(new Resolved("acme-clinic"));
    }

    @Test
    void rejectsTokenTenantThatDiffersFromTheSubdomain() {
        assertThat(resolver.fromToken("acme-clinic.localhost", List.of("bella-salon")))
                .isEqualTo(new Unresolved(Reason.CONFLICT));
    }

    @Test
    void rejectsTokensWithoutExactlyOneValidTenant() {
        assertThat(resolver.fromToken("localhost", List.of())).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.fromToken("localhost", List.of("acme-clinic", "bella-salon")))
                .isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.fromToken("localhost", List.of("Acme_Clinic"))).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.fromToken("a.b.localhost", List.of("acme-clinic"))).isEqualTo(new Unresolved(Reason.INVALID));
    }
}
