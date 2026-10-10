package dev.agenda.gateway.migration;

import java.util.List;

public record MigrationTableView(List<TenantView> tenants, List<RouteView> routes) {

    public record TenantView(String id) {
    }

    public record RouteView(String path, String target, List<String> migratedTenants) {
    }
}
