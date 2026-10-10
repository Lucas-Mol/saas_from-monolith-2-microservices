package dev.agenda.gateway.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.agenda.gateway.migration.MigrationProperties.Route;
import dev.agenda.gateway.migration.MigrationProperties.Tenant;

class RoutingTableTest {

    private static final URI ACME_LEGACY = URI.create("http://legacy-acme:8080");
    private static final URI BELLA_LEGACY = URI.create("http://legacy-bella:8080");
    private static final URI SCHEDULING = URI.create("http://scheduling-service:8080");

    private static final Route SCHEDULING_FOR_ACME =
            new Route("/api/appointments/**", SCHEDULING, null, List.of("acme-clinic"));

    @Test
    void routesEveryTenantToItsLegacyInstanceWhenNothingIsMigrated() {
        RoutingTable table = table(List.of());

        assertThat(table.resolve("acme-clinic", "/api/appointments"))
                .contains(new RouteTarget("legacy", ACME_LEGACY));
        assertThat(table.resolve("bella-salon", "/backoffice/appointments"))
                .contains(new RouteTarget("legacy", BELLA_LEGACY));
    }

    @Test
    void routesMigratedTenantToTheNewServiceOnMatchingPaths() {
        RoutingTable table = table(List.of(SCHEDULING_FOR_ACME));

        assertThat(table.resolve("acme-clinic", "/api/appointments"))
                .contains(new RouteTarget("scheduling-service", SCHEDULING));
        assertThat(table.resolve("acme-clinic", "/api/appointments/42/cancel"))
                .contains(new RouteTarget("scheduling-service", SCHEDULING));
    }

    @Test
    void keepsTenantsThatAreNotMigratedOnTheLegacyInstance() {
        RoutingTable table = table(List.of(SCHEDULING_FOR_ACME));

        assertThat(table.resolve("bella-salon", "/api/appointments"))
                .contains(new RouteTarget("legacy", BELLA_LEGACY));
    }

    @Test
    void keepsMigratedTenantOnLegacyForPathsOutsideTheRoute() {
        RoutingTable table = table(List.of(SCHEDULING_FOR_ACME));

        assertThat(table.resolve("acme-clinic", "/api/invoices"))
                .contains(new RouteTarget("legacy", ACME_LEGACY));
        assertThat(table.resolve("acme-clinic", "/api/appointmentsx"))
                .contains(new RouteTarget("legacy", ACME_LEGACY));
    }

    @Test
    void usesTheConfiguredTargetNameWhenPresent() {
        Route named = new Route("/api/invoices/**", URI.create("http://billing:8080"), "billing-service",
                List.of("bella-salon"));

        assertThat(table(List.of(named)).resolve("bella-salon", "/api/invoices"))
                .contains(new RouteTarget("billing-service", URI.create("http://billing:8080")));
    }

    @Test
    void returnsEmptyForUnknownTenants() {
        assertThat(table(List.of(SCHEDULING_FOR_ACME)).resolve("unknown-clinic", "/api/appointments")).isEmpty();
    }

    @Test
    void rejectsRoutesThatMigrateUnknownTenants() {
        Route route = new Route("/api/appointments/**", SCHEDULING, null, List.of("typo-clinic"));

        assertThatThrownBy(() -> table(List.of(route)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typo-clinic");
    }

    @Test
    void rejectsTargetsThatAreNotPlainBaseUris() {
        Route withPath = new Route("/api/appointments/**", URI.create("http://scheduling-service:8080/v1"), null,
                List.of());

        assertThatThrownBy(() -> table(List.of(withPath))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTenantIdsThatCannotBeResolvedFromRequests() {
        MigrationProperties properties = new MigrationProperties(Map.of("Acme_Clinic", new Tenant(ACME_LEGACY)),
                List.of());

        assertThatThrownBy(() -> new RoutingTable(properties)).isInstanceOf(IllegalArgumentException.class);
    }

    private static RoutingTable table(List<Route> routes) {
        return new RoutingTable(new MigrationProperties(
                Map.of("acme-clinic", new Tenant(ACME_LEGACY), "bella-salon", new Tenant(BELLA_LEGACY)), routes));
    }
}
