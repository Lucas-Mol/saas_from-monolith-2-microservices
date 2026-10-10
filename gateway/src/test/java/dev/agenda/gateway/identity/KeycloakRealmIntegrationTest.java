package dev.agenda.gateway.identity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import dasniko.testcontainers.keycloak.KeycloakContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakRealmIntegrationTest {

    private static final String PASSWORD = "dev-password";

    private static final KeycloakContainer KEYCLOAK = new KeycloakContainer(System.getProperty("keycloak.image"))
            .withRealmImportFile("identity/realm-export.json");
    private static final WireMockServer ACME = new WireMockServer(options().dynamicPort());

    static {
        KEYCLOAK.start();
        ACME.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakRealmIntegrationTest::issuer);
        registry.add("migration.tenants.acme-clinic.legacy-uri", ACME::baseUrl);
    }

    @BeforeAll
    static void createTestClients() {
        RealmResource realm = KEYCLOAK.getKeycloakAdminClient().realm("agenda");
        createPasswordGrantCopy(realm, "web", "test-web");
        createPasswordGrantCopy(realm, "backoffice-acme", "test-backoffice");
        ACME.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("acme")));
    }

    @AfterAll
    static void stop() {
        ACME.stop();
        KEYCLOAK.stop();
    }

    @Autowired
    private WebTestClient client;

    @Test
    void issuesTenantRolesAndAudienceForTenantUsers() throws Exception {
        assertClaims(token("test-web", "staff@acme.test"), List.of("acme-clinic"), List.of("STAFF"));
        assertClaims(token("test-web", "admin@acme.test"), List.of("acme-clinic"), List.of("ADMIN"));
        assertClaims(token("test-web", "alice@example.test"), List.of("acme-clinic"), List.of("CUSTOMER"));
        assertClaims(token("test-web", "admin@bella.test"), List.of("bella-salon"), List.of("ADMIN"));
        assertClaims(token("test-web", "staff@bella.test"), List.of("bella-salon"), List.of("STAFF"));
    }

    @Test
    void issuesPlatformAdminWithoutTenant() throws Exception {
        JWTClaimsSet claims = claims(token("test-web", "ops@agenda.test"));

        assertThat(claims.getClaim("tenant_id")).isNull();
        assertThat(roles(claims)).containsExactly("PLATFORM_ADMIN");
    }

    @Test
    void putsTenantAndRolesInTheBackofficeIdToken() throws Exception {
        JWTClaimsSet idToken = claims(tokens("test-backoffice", "staff@acme.test").get("id_token").toString());

        assertThat(idToken.getStringListClaim("tenant_id")).containsExactly("acme-clinic");
        assertThat(roles(idToken)).containsExactly("STAFF");
    }

    @Test
    void neverIssuesASingleTenantForMembersOfSeveralOrganizations() throws Exception {
        RealmResource realm = KEYCLOAK.getKeycloakAdminClient().realm("agenda");
        UserRepresentation user = new UserRepresentation();
        user.setUsername("both@test.local");
        user.setEmail("both@test.local");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFirstName("Both");
        user.setLastName("Orgs");
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(PASSWORD);
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        realm.users().create(user).close();
        String userId = realm.users().searchByUsername("both@test.local", true).get(0).getId();
        for (OrganizationRepresentation organization : realm.organizations().getAll()) {
            realm.organizations().get(organization.getId()).members().addMember(userId).close();
        }

        String defaultToken = token("test-web", "both@test.local");
        assertThat(claims(defaultToken).getClaim("tenant_id")).isNull();
        expectApiStatus(defaultToken, 403);

        for (String scope : List.of("openid organization:bella-salon", "openid organization:*")) {
            Object token = tokensWithScope("test-web", "both@test.local", scope).get("access_token");
            assertThat(token).isInstanceOf(String.class);
            expectApiStatus((String) token, 401);
        }
    }

    private void expectApiStatus(String token, int status) {
        client.get().uri("/api/customers")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody().jsonPath("$.status").isEqualTo(status);
    }

    @Test
    void gatewayAcceptsRealTokensAndRoutesByTheirTenant() {
        String token = token("test-web", "staff@acme.test");

        client.get().uri("/api/customers")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Routed-To", "legacy")
                .expectBody(String.class).isEqualTo("acme");

        ACME.verify(getRequestedFor(urlEqualTo("/api/customers")).withHeader("Authorization", equalTo("Bearer " + token)));
    }

    @Test
    void gatewayRejectsTamperedTokensAndTokensForOtherAudiences() {
        String token = token("test-web", "staff@acme.test");
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        String backofficeToken = token("test-backoffice", "staff@acme.test");

        for (String rejected : List.of(tampered, backofficeToken)) {
            client.get().uri("/api/customers")
                    .headers(h -> h.setBearerAuth(rejected))
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody().jsonPath("$.status").isEqualTo(401);
        }
    }

    @Test
    void gatewayExposesMigrationTableOnlyToPlatformAdmins() {
        client.get().uri("/admin/migration")
                .headers(h -> h.setBearerAuth(token("test-web", "ops@agenda.test")))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.tenants").isArray();

        client.get().uri("/admin/migration")
                .headers(h -> h.setBearerAuth(token("test-web", "admin@acme.test")))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.status").isEqualTo(403);
    }

    private static void assertClaims(String token, List<String> tenants, List<String> roles) throws ParseException {
        JWTClaimsSet claims = claims(token);
        assertThat(claims.getIssuer()).isEqualTo(issuer());
        assertThat(claims.getAudience()).contains("gateway");
        assertThat(claims.getStringListClaim("tenant_id")).isEqualTo(tenants);
        assertThat(roles(claims)).isEqualTo(roles);
    }

    @SuppressWarnings("unchecked")
    private static List<String> roles(JWTClaimsSet claims) throws ParseException {
        Map<String, Object> realmAccess = claims.getJSONObjectClaim("realm_access");
        return (List<String>) realmAccess.get("roles");
    }

    private static JWTClaimsSet claims(String token) throws ParseException {
        return SignedJWT.parse(token).getJWTClaimsSet();
    }

    private static String token(String clientId, String username) {
        return tokens(clientId, username).get("access_token").toString();
    }

    private static Map<?, ?> tokens(String clientId, String username) {
        Map<?, ?> response = tokensWithScope(clientId, username, "openid");
        assertThat(response.get("access_token")).as("token response %s", response).isNotNull();
        return response;
    }

    private static Map<?, ?> tokensWithScope(String clientId, String username, String scope) {
        String form = Map.of("grant_type", "password", "client_id", clientId, "client_secret", "test-secret",
                        "username", username, "password", PASSWORD, "scope", scope)
                .entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(issuer() + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new ObjectMapper().readValue(response.body(), Map.class);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void createPasswordGrantCopy(RealmResource realm, String sourceClientId, String clientId) {
        ClientRepresentation copy = realm.clients().findByClientId(sourceClientId).get(0);
        copy.setId(null);
        copy.setClientId(clientId);
        copy.setPublicClient(false);
        copy.setSecret("test-secret");
        copy.setDirectAccessGrantsEnabled(true);
        if (copy.getProtocolMappers() != null) {
            copy.getProtocolMappers().forEach(mapper -> mapper.setId(null));
        }
        realm.clients().create(copy).close();
    }

    private static String issuer() {
        return KEYCLOAK.getAuthServerUrl() + "/realms/agenda";
    }
}
