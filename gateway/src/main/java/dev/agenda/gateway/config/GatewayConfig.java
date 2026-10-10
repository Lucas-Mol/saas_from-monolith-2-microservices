package dev.agenda.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.agenda.gateway.migration.MigrationProperties;
import dev.agenda.gateway.migration.RoutingTable;
import dev.agenda.gateway.routing.GatewayErrorHandler;
import dev.agenda.gateway.routing.ProblemResponseWriter;
import dev.agenda.gateway.routing.TenantMigrationRoutingFilter;
import dev.agenda.gateway.tenant.TenantProperties;
import dev.agenda.gateway.tenant.TenantResolver;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MigrationProperties.class, TenantProperties.class})
public class GatewayConfig {

    @Bean
    RoutingTable routingTable(MigrationProperties properties) {
        return new RoutingTable(properties);
    }

    @Bean
    TenantResolver tenantResolver(TenantProperties properties) {
        return new TenantResolver(properties.baseDomain());
    }

    @Bean
    ProblemResponseWriter problemResponseWriter(ObjectMapper objectMapper) {
        return new ProblemResponseWriter(objectMapper);
    }

    @Bean
    GatewayErrorHandler gatewayErrorHandler(ProblemResponseWriter problems) {
        return new GatewayErrorHandler(problems);
    }

    @Bean
    TenantMigrationRoutingFilter tenantMigrationRoutingFilter(TenantResolver tenantResolver, RoutingTable routingTable,
                                                              ProblemResponseWriter problems) {
        return new TenantMigrationRoutingFilter(tenantResolver, routingTable, problems);
    }

    @Bean
    RouteLocator tenantMigrationRoute(RouteLocatorBuilder routes, TenantMigrationRoutingFilter routingFilter) {
        return routes.routes()
                .route("tenant-migration", route -> route.not(path -> path.path("/actuator/**"))
                        .filters(filters -> filters.preserveHostHeader().filter(routingFilter))
                        .uri("no://op"))
                .build();
    }
}
