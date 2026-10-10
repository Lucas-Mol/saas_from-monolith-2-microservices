package dev.agenda.gateway.migration;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("migration")
public record MigrationProperties(Map<String, @Valid Tenant> tenants, List<@Valid Route> routes) {

    public MigrationProperties {
        tenants = tenants == null ? Map.of() : Map.copyOf(tenants);
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public record Tenant(@NotNull URI legacyUri) {
    }

    public record Route(@NotBlank String path, @NotNull URI target, String name, List<String> migratedTenants) {

        public Route {
            if ((name == null || name.isBlank()) && target != null) {
                name = target.getHost();
            }
            migratedTenants = migratedTenants == null ? List.of() : List.copyOf(migratedTenants);
        }
    }
}
