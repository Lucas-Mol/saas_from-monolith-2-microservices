package dev.agenda.gateway.routing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String ROUTED_TO = "X-Routed-To";

    private static final WireMockServer ACME = new WireMockServer(options().dynamicPort());
    private static final WireMockServer BELLA = new WireMockServer(options().dynamicPort());
    private static final WireMockServer SCHEDULING = new WireMockServer(options().dynamicPort());
    private static final List<WireMockServer> UPSTREAMS = List.of(ACME, BELLA, SCHEDULING);
    private static final int CLOSED_PORT = findClosedPort();

    static {
        UPSTREAMS.forEach(WireMockServer::start);
    }

    @DynamicPropertySource
    static void routing(DynamicPropertyRegistry registry) {
        registry.add("migration.tenants.acme-clinic.legacy-uri", ACME::baseUrl);
        registry.add("migration.tenants.bella-salon.legacy-uri", BELLA::baseUrl);
        registry.add("migration.tenants.offline-clinic.legacy-uri", () -> "http://localhost:" + CLOSED_PORT);
        registry.add("migration.tenants.unresolvable-clinic.legacy-uri", () -> "http://unresolvable.invalid:8080");
        registry.add("spring.cloud.gateway.server.webflux.httpclient.response-timeout", () -> "1s");
        registry.add("migration.routes[0].path", () -> "/api/appointments/**");
        registry.add("migration.routes[0].target", SCHEDULING::baseUrl);
        registry.add("migration.routes[0].name", () -> "scheduling-service");
        registry.add("migration.routes[0].migrated-tenants[0]", () -> "acme-clinic");
    }

    @AfterAll
    static void stopUpstreams() {
        UPSTREAMS.forEach(WireMockServer::stop);
    }

    @Autowired
    private WebTestClient client;

    @LocalServerPort
    private int port;

    @BeforeEach
    void stubUpstreams() {
        ACME.resetAll();
        BELLA.resetAll();
        SCHEDULING.resetAll();
        ACME.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("acme")));
        BELLA.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("bella")));
        SCHEDULING.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("scheduling")));
    }

    @Test
    void routesTenantFromSubdomainToItsLegacyInstanceKeepingPathQueryAndHost() {
        client.get().uri("/api/customers?active=true")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ROUTED_TO, "legacy")
                .expectBody(String.class).isEqualTo("acme");

        ACME.verify(getRequestedFor(urlEqualTo("/api/customers?active=true"))
                .withHeader(HttpHeaders.HOST, equalTo("acme-clinic.localhost")));
        BELLA.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void routesTenantFromHeaderAndDoesNotForwardTheHeader() {
        client.get().uri("/api/customers")
                .header(TENANT_HEADER, "bella-salon")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ROUTED_TO, "legacy")
                .expectBody(String.class).isEqualTo("bella");

        BELLA.verify(getRequestedFor(urlEqualTo("/api/customers")).withoutHeader(TENANT_HEADER));
        ACME.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void forwardsMethodBodyAndCredentials() {
        client.post().uri("/api/customers")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .header(HttpHeaders.AUTHORIZATION, "Basic c3RhZmY6c2VjcmV0")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fullName\":\"Diego\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("acme");

        ACME.verify(postRequestedFor(urlEqualTo("/api/customers"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic c3RhZmY6c2VjcmV0"))
                .withRequestBody(equalToJson("{\"fullName\":\"Diego\"}")));
    }

    @Test
    void passesLegacyRedirectsCookiesAndErrorsThroughUnchanged() {
        ACME.stubFor(get("/backoffice").willReturn(aResponse().withStatus(302)
                .withHeader(HttpHeaders.LOCATION, "http://acme-clinic.localhost/login")
                .withHeader(HttpHeaders.SET_COOKIE, "ACME_SESSION=abc; Path=/; HttpOnly")));
        String legacyError = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Customer 9 not found\"}";
        ACME.stubFor(get("/api/customers/9").willReturn(aResponse().withStatus(404)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(legacyError)));

        client.get().uri("/backoffice")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "http://acme-clinic.localhost/login")
                .expectHeader().valueEquals(HttpHeaders.SET_COOKIE, "ACME_SESSION=abc; Path=/; HttpOnly")
                .expectBody().isEmpty();

        client.get().uri("/api/customers/9")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().valueEquals(ROUTED_TO, "legacy")
                .expectBody().json(legacyError);
    }

    @Test
    void routesMigratedTenantToTheNewServiceAndKeepsOthersOnLegacy() {
        client.get().uri("/api/appointments")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ROUTED_TO, "scheduling-service")
                .expectBody(String.class).isEqualTo("scheduling");

        client.get().uri("/api/appointments")
                .header(HttpHeaders.HOST, "bella-salon.localhost")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(ROUTED_TO, "legacy")
                .expectBody(String.class).isEqualTo("bella");

        ACME.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void rejectsRequestsWithoutTenant() {
        expectForbidden(client.get().uri("/api/customers").exchange());
    }

    @Test
    void rejectsUnknownTenant() {
        expectForbidden(client.get().uri("/api/customers").header(TENANT_HEADER, "unknown-clinic").exchange());
    }

    @Test
    void rejectsConflictingTenantIdentifiers() {
        expectForbidden(client.get().uri("/api/customers")
                .header(HttpHeaders.HOST, "acme-clinic.localhost")
                .header(TENANT_HEADER, "bella-salon")
                .exchange());
    }

    @Test
    void rejectsPathTraversalAttempts() {
        for (String path : List.of("/api/appointments/../invoices", "/api/appointments/%2e%2e/invoices",
                "/api/appointments%2F..%2Finvoices", "/api/appointments;x=1",
                "/backoffice/..%3b/x", "/api//appointments", "/api/appointments/%5c..", "/api/%25")) {
            client.get().uri(URI.create("http://localhost:" + port + path))
                    .header(TENANT_HEADER, "acme-clinic")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody().jsonPath("$.status").isEqualTo(400);
        }
        UPSTREAMS.forEach(upstream -> upstream.verify(0, anyRequestedFor(anyUrl())));
    }

    @Test
    void servesHealthLocallyWithoutTenant() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(ROUTED_TO)
                .expectBody().jsonPath("$.status").isEqualTo("UP");

        UPSTREAMS.forEach(upstream -> upstream.verify(0, anyRequestedFor(anyUrl())));
    }

    @Test
    void reportsUnreachableUpstreamsAsBadGateway() {
        for (String tenant : List.of("offline-clinic", "unresolvable-clinic")) {
            client.get().uri("/api/customers")
                    .header(TENANT_HEADER, tenant)
                    .exchange()
                    .expectStatus().isEqualTo(502)
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectHeader().valueEquals(ROUTED_TO, "legacy")
                    .expectBody().jsonPath("$.status").isEqualTo(502);
        }
    }

    @Test
    void reportsSlowUpstreamAsGatewayTimeout() {
        ACME.stubFor(get("/api/slow").willReturn(aResponse().withStatus(200).withFixedDelay(3000)));

        client.get().uri("/api/slow")
                .header(TENANT_HEADER, "acme-clinic")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(504);
    }

    @Test
    void doesNotLetUpstreamsSpoofTheRoutingHeader() {
        ACME.stubFor(get("/api/customers").willReturn(aResponse().withStatus(200).withHeader(ROUTED_TO, "evil")));

        client.get().uri("/api/customers")
                .header(TENANT_HEADER, "acme-clinic")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().values(ROUTED_TO, values -> assertThat(values)
                        .containsExactly("legacy"))
                .expectBody().isEmpty();
    }

    private static void expectForbidden(WebTestClient.ResponseSpec response) {
        response.expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().doesNotExist(ROUTED_TO)
                .expectBody().jsonPath("$.status").isEqualTo(403);
        UPSTREAMS.forEach(upstream -> upstream.verify(0, anyRequestedFor(anyUrl())));
    }

    private static int findClosedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
