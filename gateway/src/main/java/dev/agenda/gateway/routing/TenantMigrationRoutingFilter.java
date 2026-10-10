package dev.agenda.gateway.routing;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.containsEncodedParts;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import dev.agenda.gateway.migration.RouteTarget;
import dev.agenda.gateway.migration.RoutingTable;
import dev.agenda.gateway.tenant.TenantResolution;
import dev.agenda.gateway.tenant.TenantResolver;

import reactor.core.publisher.Mono;

public final class TenantMigrationRoutingFilter implements GatewayFilter, Ordered {

    public static final String ROUTED_TO_HEADER = "X-Routed-To";

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationRoutingFilter.class);

    private static final List<String> AMBIGUOUS_PATH_TOKENS = List.of("%2e", "%2f", "%5c", "%25", "%3b", "\\", ";", "//");

    private final TenantResolver tenantResolver;
    private final RoutingTable routingTable;
    private final ProblemResponseWriter problems;

    public TenantMigrationRoutingFilter(TenantResolver tenantResolver, RoutingTable routingTable,
                                        ProblemResponseWriter problems) {
        this.tenantResolver = tenantResolver;
        this.routingTable = routingTable;
        this.problems = problems;
    }

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isAmbiguous(request.getPath())) {
            return problems.write(exchange, HttpStatus.BAD_REQUEST, "Request path is not allowed");
        }

        TenantResolution resolution = tenantResolver.resolve(request.getHeaders().getFirst(HttpHeaders.HOST),
                request.getHeaders().getOrEmpty(TenantResolver.TENANT_HEADER));
        if (!(resolution instanceof TenantResolution.Resolved(String tenantId))) {
            return problems.write(exchange, HttpStatus.FORBIDDEN,
                    describe(((TenantResolution.Unresolved) resolution).reason()));
        }

        Optional<RouteTarget> target = routingTable.resolve(tenantId, request.getPath().pathWithinApplication().value());
        if (target.isEmpty()) {
            return problems.write(exchange, HttpStatus.FORBIDDEN, "Unknown tenant");
        }
        RouteTarget routeTarget = target.get();

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, targetUrl(request.getURI(), routeTarget.uri()));
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(ROUTED_TO_HEADER, routeTarget.name());
            return Mono.empty();
        });
        log.info("tenant={} method={} path={} routedTo={}", tenantId, request.getMethod(),
                request.getPath().value(), routeTarget.name());

        ServerWebExchange forwarded = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.remove(TenantResolver.TENANT_HEADER)))
                .build();
        return chain.filter(forwarded);
    }

    private static boolean isAmbiguous(RequestPath path) {
        String raw = path.value().toLowerCase(Locale.ROOT);
        if (AMBIGUOUS_PATH_TOKENS.stream().anyMatch(raw::contains)) {
            return true;
        }
        return path.elements().stream()
                .filter(PathContainer.PathSegment.class::isInstance)
                .map(element -> ((PathContainer.PathSegment) element).value())
                .anyMatch(segment -> segment.equals(".") || segment.equals(".."));
    }

    private static String describe(TenantResolution.Reason reason) {
        return switch (reason) {
            case MISSING -> "Request does not identify a tenant";
            case INVALID -> "Tenant identifier is malformed";
            case CONFLICT -> "Conflicting tenant identifiers";
        };
    }

    private static URI targetUrl(URI requestUri, URI target) {
        return UriComponentsBuilder.fromUri(requestUri)
                .scheme(target.getScheme())
                .host(target.getHost())
                .port(target.getPort())
                .build(containsEncodedParts(requestUri))
                .toUri();
    }
}
