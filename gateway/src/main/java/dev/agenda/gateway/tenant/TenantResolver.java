package dev.agenda.gateway.tenant;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import dev.agenda.gateway.tenant.TenantResolution.Reason;
import dev.agenda.gateway.tenant.TenantResolution.Resolved;
import dev.agenda.gateway.tenant.TenantResolution.Unresolved;

public final class TenantResolver {

    private static final Pattern TENANT_ID = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    private final String hostSuffix;

    public TenantResolver(String baseDomain) {
        if (baseDomain == null || baseDomain.isBlank()) {
            throw new IllegalArgumentException("Base domain is required");
        }
        this.hostSuffix = "." + baseDomain.toLowerCase(Locale.ROOT);
    }

    public static boolean isValidTenantId(String tenantId) {
        return tenantId != null && TENANT_ID.matcher(tenantId).matches();
    }

    public TenantResolution fromHost(String host) {
        String label = subdomainOf(host);
        if (label == null) {
            return new Unresolved(Reason.MISSING);
        }
        return isValidTenantId(label) ? new Resolved(label) : new Unresolved(Reason.INVALID);
    }

    public TenantResolution fromToken(String host, List<String> tokenTenants) {
        if (tokenTenants.isEmpty()) {
            return new Unresolved(Reason.MISSING);
        }
        if (tokenTenants.size() > 1 || !isValidTenantId(tokenTenants.get(0))) {
            return new Unresolved(Reason.INVALID);
        }
        String tenantId = tokenTenants.get(0);
        return switch (fromHost(host)) {
            case Resolved(String hostTenant) when !hostTenant.equals(tenantId) -> new Unresolved(Reason.CONFLICT);
            case Unresolved(Reason reason) when reason == Reason.INVALID -> new Unresolved(Reason.INVALID);
            default -> new Resolved(tenantId);
        };
    }

    private String subdomainOf(String host) {
        if (host == null || host.isBlank() || host.startsWith("[")) {
            return null;
        }
        String name = host.toLowerCase(Locale.ROOT);
        int portSeparator = name.lastIndexOf(':');
        if (portSeparator >= 0) {
            name = name.substring(0, portSeparator);
        }
        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        if (!name.endsWith(hostSuffix)) {
            return null;
        }
        return name.substring(0, name.length() - hostSuffix.length());
    }
}
