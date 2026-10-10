package dev.agenda.legacy.users;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class TokenClaims {

    public static final String TENANT = "tenant_id";

    private TokenClaims() {
    }

    public static List<String> tenants(Map<String, Object> claims) {
        Object value = claims.get(TENANT);
        if (value instanceof String tenant) {
            return List.of(tenant);
        }
        return stringsOrEmpty(value);
    }

    public static Collection<String> scopes(String scopeClaim) {
        return scopeClaim == null ? List.of() : Arrays.asList(scopeClaim.split(" "));
    }

    public static boolean narrowedToOneOrganization(Collection<String> scopes) {
        return scopes.stream().anyMatch(scope -> scope.startsWith("organization:"));
    }

    public static boolean emailVerified(Map<String, Object> claims) {
        return Boolean.TRUE.equals(claims.get("email_verified"));
    }

    public static List<String> realmRoles(Map<String, Object> claims) {
        if (claims.get("realm_access") instanceof Map<?, ?> realmAccess) {
            return stringsOrEmpty(realmAccess.get("roles"));
        }
        return List.of();
    }

    private static List<String> stringsOrEmpty(Object value) {
        if (value instanceof Collection<?> values && values.stream().allMatch(String.class::isInstance)) {
            return values.stream().map(String.class::cast).toList();
        }
        return List.of();
    }
}
