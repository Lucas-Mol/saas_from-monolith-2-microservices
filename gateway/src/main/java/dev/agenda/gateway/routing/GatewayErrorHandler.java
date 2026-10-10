package dev.agenda.gateway.routing;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

public final class GatewayErrorHandler implements ErrorWebExceptionHandler, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private static final List<Class<? extends Throwable>> UPSTREAM_FAILURES =
            List.of(ConnectException.class, UnknownHostException.class, PrematureCloseException.class);

    private final ProblemResponseWriter problems;

    public GatewayErrorHandler(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        String request = exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath().value();
        if (UPSTREAM_FAILURES.stream().anyMatch(type -> causedBy(ex, type))) {
            log.warn("Upstream request failed for {}: {}", request, ex.getMessage());
            return problems.write(exchange, HttpStatus.BAD_GATEWAY, "Upstream service is unavailable");
        }
        if (ex instanceof ResponseStatusException statusException) {
            HttpStatus status = HttpStatus.resolve(statusException.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            if (status.is5xxServerError()) {
                log.warn("Gateway error {} for {}: {}", status.value(), request, ex.getMessage());
            }
            return problems.write(exchange, status, status.getReasonPhrase());
        }
        log.error("Unexpected gateway error for {}", request, ex);
        return problems.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private static boolean causedBy(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
