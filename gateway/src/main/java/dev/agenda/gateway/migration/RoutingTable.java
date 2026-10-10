package dev.agenda.gateway.migration;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import dev.agenda.gateway.migration.MigrationProperties.Route;
import dev.agenda.gateway.tenant.TenantResolver;

public final class RoutingTable {

    public static final String LEGACY = "legacy";

    private final Map<String, URI> legacyUris;
    private final List<MigratedRoute> routes;

    public RoutingTable(MigrationProperties properties) {
        this.legacyUris = properties.tenants().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                entry -> requireValidTenantId(entry.getKey()),
                entry -> requireBaseUri(entry.getValue().legacyUri(), "tenant " + entry.getKey())));
        this.routes = properties.routes().stream().map(this::compile).toList();
    }

    public Optional<RouteTarget> resolve(String tenantId, String path) {
        URI legacyUri = legacyUris.get(tenantId);
        if (legacyUri == null) {
            return Optional.empty();
        }
        PathContainer pathContainer = PathContainer.parsePath(path);
        RouteTarget target = routes.stream()
                .filter(route -> route.migratedTenants().contains(tenantId) && route.pattern().matches(pathContainer))
                .map(MigratedRoute::target)
                .findFirst()
                .orElse(new RouteTarget(LEGACY, legacyUri));
        return Optional.of(target);
    }

    public MigrationTableView view() {
        List<MigrationTableView.TenantView> tenants = legacyUris.keySet().stream().sorted()
                .map(MigrationTableView.TenantView::new)
                .toList();
        List<MigrationTableView.RouteView> routeViews = routes.stream()
                .map(route -> new MigrationTableView.RouteView(route.pattern().getPatternString(), route.target().name(),
                        route.migratedTenants().stream().sorted().toList()))
                .toList();
        return new MigrationTableView(tenants, routeViews);
    }

    private MigratedRoute compile(Route route) {
        for (String tenantId : route.migratedTenants()) {
            if (!legacyUris.containsKey(tenantId)) {
                throw new IllegalArgumentException("Route " + route.path() + " migrates unknown tenant " + tenantId);
            }
        }
        return new MigratedRoute(PathPatternParser.defaultInstance.parse(route.path()),
                Set.copyOf(route.migratedTenants()),
                new RouteTarget(route.name(), requireBaseUri(route.target(), "route " + route.path())));
    }

    private static String requireValidTenantId(String tenantId) {
        if (!TenantResolver.isValidTenantId(tenantId)) {
            throw new IllegalArgumentException("Invalid tenant id in migration table: " + tenantId);
        }
        return tenantId;
    }

    private static URI requireBaseUri(URI uri, String owner) {
        boolean httpScheme = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
        boolean emptyPath = uri.getRawPath() == null || uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath());
        if (!httpScheme || uri.getHost() == null || !emptyPath || uri.getRawQuery() != null
                || uri.getRawFragment() != null || uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("Target of " + owner + " must be a plain http(s)://host:port URI");
        }
        return uri;
    }

    private record MigratedRoute(PathPattern pattern, Set<String> migratedTenants, RouteTarget target) {
    }
}
