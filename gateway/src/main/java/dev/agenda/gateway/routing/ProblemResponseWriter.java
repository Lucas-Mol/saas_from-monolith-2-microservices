package dev.agenda.gateway.routing;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

public final class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(ProblemDetail.forStatusAndDetail(status, detail)))
                .flatMap(body -> {
                    response.setStatusCode(status);
                    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                    response.getHeaders().setContentLength(body.length);
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
                });
    }
}
