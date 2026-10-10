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
    void resolvesTenantFromSubdomain() {
        assertThat(resolver.resolve("acme-clinic.localhost:8080", List.of())).isEqualTo(new Resolved("acme-clinic"));
    }

    @Test
    void ignoresHostCaseAndTrailingDot() {
        assertThat(resolver.resolve("ACME-Clinic.LocalHost.", List.of())).isEqualTo(new Resolved("acme-clinic"));
    }

    @Test
    void resolvesTenantFromHeader() {
        assertThat(resolver.resolve("localhost:8080", List.of("bella-salon"))).isEqualTo(new Resolved("bella-salon"));
    }

    @Test
    void acceptsMatchingSubdomainAndHeader() {
        assertThat(resolver.resolve("acme-clinic.localhost", List.of("acme-clinic")))
                .isEqualTo(new Resolved("acme-clinic"));
    }

    @Test
    void rejectsConflictingSubdomainAndHeader() {
        assertThat(resolver.resolve("acme-clinic.localhost", List.of("bella-salon")))
                .isEqualTo(new Unresolved(Reason.CONFLICT));
    }

    @Test
    void reportsMissingTenantWhenNothingIdentifiesIt() {
        assertThat(resolver.resolve("localhost:8080", List.of())).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.resolve(null, List.of())).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.resolve("[::1]:8080", List.of())).isEqualTo(new Unresolved(Reason.MISSING));
    }

    @Test
    void ignoresHostsOutsideTheBaseDomain() {
        assertThat(resolver.resolve("acme-clinic.example.com", List.of())).isEqualTo(new Unresolved(Reason.MISSING));
        assertThat(resolver.resolve("acme-clinic-localhost", List.of())).isEqualTo(new Unresolved(Reason.MISSING));
    }

    @Test
    void rejectsMalformedTenantIds() {
        assertThat(resolver.resolve("localhost", List.of("Acme_Clinic"))).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.resolve("localhost", List.of(""))).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.resolve("localhost", List.of("-acme"))).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.resolve("localhost", List.of("a".repeat(64)))).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.resolve("a.b.localhost", List.of())).isEqualTo(new Unresolved(Reason.INVALID));
        assertThat(resolver.resolve(".localhost", List.of())).isEqualTo(new Unresolved(Reason.INVALID));
    }

    @Test
    void rejectsRepeatedTenantHeaders() {
        assertThat(resolver.resolve("localhost", List.of("acme-clinic", "acme-clinic")))
                .isEqualTo(new Unresolved(Reason.INVALID));
    }
}
