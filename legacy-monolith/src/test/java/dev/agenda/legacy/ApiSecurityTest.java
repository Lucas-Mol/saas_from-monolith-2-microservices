package dev.agenda.legacy;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.agenda.legacy.support.IntegrationTest;
import dev.agenda.legacy.support.TestJwts;

@IntegrationTest
class ApiSecurityTest {

    private static RequestPostProcessor bearer(String user) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.forUser(user));
            return request;
        };
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotSendAuthenticationChallenge() throws Exception {
        mvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void rejectsStateChangingRequestsWithFormContentTypes() throws Exception {
        for (MediaType type : new MediaType[] {MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA,
                MediaType.TEXT_PLAIN}) {
            mvc.perform(post("/api/invoices/1/pay").with(bearer("admin@test.local")).contentType(type))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Test
    void acceptsBodylessStateChangingRequests() throws Exception {
        mvc.perform(post("/api/appointments/2/confirm").with(bearer("staff@test.local")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsTokensThatFailValidation() throws Exception {
        for (String token : List.of(
                TestJwts.token().email("staff@test.local").roles("STAFF").tenant(List.of("other-tenant")).build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").tenant(null).build(),
                TestJwts.token().email("staff@test.local").roles("STAFF")
                        .tenant(List.of(TestJwts.TENANT, "other-tenant")).build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").audience("account").build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").issuer("http://evil.test/realms/agenda").build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").expiresAt(Instant.now().minusSeconds(120)).build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").signedWithUnknownKey().build(),
                TestJwts.token().email("staff@test.local").roles("STAFF").scope("openid organization:test-tenant").build(),
                "not-a-jwt")) {
            mvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void linksCustomerRecordsOnlyThroughVerifiedEmail() throws Exception {
        String unverified = TestJwts.token().email("ana.user@test.local").roles("CUSTOMER").emailVerified(false).build();
        mvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, "Bearer " + unverified)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsLegacyBasicCredentials() throws Exception {
        mvc.perform(get("/api/customers").with(httpBasic("staff@test.local", "test-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsSingleStringTenantClaim() throws Exception {
        String token = TestJwts.token().email("staff@test.local").roles("STAFF").tenant(TestJwts.TENANT).build();
        mvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void takesRolesFromTheTokenEvenForUsersUnknownLocally() throws Exception {
        String token = TestJwts.token().email("new.staff@test.local").roles("STAFF").build();
        mvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/invoices").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantsNothingToTokensWithoutLegacyRoles() throws Exception {
        String token = TestJwts.token().email("ops@agenda.test").roles("PLATFORM_ADMIN").build();
        mvc.perform(get("/api/professionals").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void doesNotCreateSessionForApiCalls() throws Exception {
        mvc.perform(get("/api/customers").with(bearer("staff@test.local")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void staffCannotAccessInvoices() throws Exception {
        mvc.perform(get("/api/invoices").with(bearer("staff@test.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotListAppointmentsOrCustomers() throws Exception {
        mvc.perform(get("/api/appointments").with(bearer("ana.user@test.local")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customers").with(bearer("ana.user@test.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotConfirmOrComplete() throws Exception {
        mvc.perform(post("/api/appointments/2/confirm").with(bearer("ana.user@test.local")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/1/complete").with(bearer("ana.user@test.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanBookForThemselves() throws Exception {
        mvc.perform(post("/api/appointments").with(bearer("ana.user@test.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotBookForSomeoneElse() throws Exception {
        mvc.perform(post("/api/appointments").with(bearer("ana.user@test.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 2, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerWithoutCustomerRecordCannotBook() throws Exception {
        mvc.perform(post("/api/appointments").with(bearer("orphan@test.local"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanCancelOnlyOwnAppointments() throws Exception {
        mvc.perform(post("/api/appointments/6/cancel").with(bearer("ana.user@test.local")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/appointments/3/cancel").with(bearer("ana.user@test.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesUnmappedApiPaths() throws Exception {
        mvc.perform(get("/api/users").with(bearer("admin@test.local")))
                .andExpect(status().isForbidden());
    }
}
