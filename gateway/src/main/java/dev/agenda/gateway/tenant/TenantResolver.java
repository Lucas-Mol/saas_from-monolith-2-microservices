package dev.agenda.gateway.tenant;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import dev.agenda.gateway.tenant.TenantResolution.Reason;
import dev.agenda.gateway.tenant.TenantResolution.Resolved;
import dev.agenda.gateway.tenant.TenantResolution.Unresolved;


public final class TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-ID";

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

    public TenantResolution resolve(String host, List<String> headerValues) {
        if (headerValues.size() > 1) {
            return new Unresolved(Reason.INVALID);
        }
        String fromHeader = headerValues.isEmpty() ? null : headerValues.get(0).trim();
        if (fromHeader != null && !isValidTenantId(fromHeader)) {
            return new Unresolved(Reason.INVALID);
        }
        String fromHost = subdomainOf(host);
        if (fromHost != null && !isValidTenantId(fromHost)) {
            return new Unresolved(Reason.INVALID);
        }
        if (fromHost != null && fromHeader != null && !fromHost.equals(fromHeader)) {
            return new Unresolved(Reason.CONFLICT);
        }
        String tenantId = fromHost != null ? fromHost : fromHeader;
        return tenantId != null ? new Resolved(tenantId) : new Unresolved(Reason.MISSING);
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
