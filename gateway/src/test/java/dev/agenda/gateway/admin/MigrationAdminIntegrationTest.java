package dev.agenda.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.agenda.gateway.support.TestJwtDecoderConfig;
import dev.agenda.gateway.support.TestJwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = {
        "migration.routes[0].path=/api/appointments/**",
        "migration.routes[0].target=http://scheduling-service:8080",
        "migration.routes[0].migrated-tenants[0]=acme-clinic"})
class MigrationAdminIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void showsTheMigrationTableToPlatformAdmins() {
        client.get().uri("/admin/migration")
                .headers(h -> h.setBearerAuth(TestJwts.token().roles("PLATFORM_ADMIN").build()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.tenants.length()").isEqualTo(2)
                .jsonPath("$.tenants[0].id").isEqualTo("acme-clinic")
                .jsonPath("$.tenants[1].id").isEqualTo("bella-salon")
                .jsonPath("$.tenants[0].legacyUri").doesNotExist()
                .jsonPath("$.routes[0].path").isEqualTo("/api/appointments/**")
                .jsonPath("$.routes[0].target").isEqualTo("scheduling-service")
                .jsonPath("$.routes[0].migratedTenants[0]").isEqualTo("acme-clinic");
    }

    @Test
    void hidesTheMigrationTableFromTenantAdmins() {
        client.get().uri("/admin/migration")
                .headers(h -> h.setBearerAuth(TestJwts.forTenant("acme-clinic", "ADMIN")))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(403);
    }

    @Test
    void requiresAuthentication() {
        client.get().uri("/admin/migration")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void neverProxiesUnknownAdminPaths() {
        client.get().uri("/admin/other")
                .header("Host", "acme-clinic.localhost")
                .headers(h -> h.setBearerAuth(TestJwts.token().roles("PLATFORM_ADMIN").build()))
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().doesNotExist("X-Routed-To")
                .expectBody().jsonPath("$.status").isEqualTo(404);
    }
}
