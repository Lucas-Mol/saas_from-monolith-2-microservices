package dev.agenda.gateway.tenant;

public sealed interface TenantResolution {

    record Resolved(String tenantId) implements TenantResolution {
    }

    record Unresolved(Reason reason) implements TenantResolution {
    }

    enum Reason {
        MISSING,
        INVALID,
        CONFLICT
    }
}
