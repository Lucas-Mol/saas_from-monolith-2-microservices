package dev.agenda.gateway.tenant;

import java.util.Collection;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

public final class TenantClaim {

    public static final String NAME = "tenant_id";

    private TenantClaim() {
    }

    public static List<String> tenantsOf(Jwt jwt) {
        Object value = jwt.getClaim(NAME);
        if (value instanceof String tenant) {
            return List.of(tenant);
        }
        if (value instanceof Collection<?> values && values.stream().allMatch(String.class::isInstance)) {
            return values.stream().map(String.class::cast).toList();
        }
        return List.of();
    }
}
