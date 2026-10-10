package dev.agenda.legacy;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.agenda.legacy.support.IntegrationTest;

@IntegrationTest
class ApiSecurityTest {

    private static final String PASSWORD = "test-password";

    @Autowired
    private MockMvc mvc;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotSendBasicChallengeSoBrowsersNeverCacheCredentials() throws Exception {
        mvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void rejectsStateChangingRequestsWithFormContentTypes() throws Exception {
        for (MediaType type : new MediaType[] {MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA,
                MediaType.TEXT_PLAIN}) {
            mvc.perform(post("/api/invoices/1/pay").with(httpBasic("admin@test.local", PASSWORD)).contentType(type))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Test
    void acceptsBodylessStateChangingRequests() throws Exception {
        mvc.perform(post("/api/appointments/2/confirm").with(httpBasic("staff@test.local", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mvc.perform(get("/api/customers").with(httpBasic("staff@test.local", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotCreateSessionForApiCalls() throws Exception {
        mvc.perform(get("/api/customers").with(httpBasic("staff@test.local", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void staffCannotAccessInvoices() throws Exception {
        mvc.perform(get("/api/invoices").with(httpBasic("staff@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotListAppointmentsOrCustomers() throws Exception {
        mvc.perform(get("/api/appointments").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customers").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotConfirmOrComplete() throws Exception {
        mvc.perform(post("/api/appointments/2/confirm").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/1/complete").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanBookForThemselves() throws Exception {
        mvc.perform(post("/api/appointments").with(httpBasic("ana.user@test.local", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotBookForSomeoneElse() throws Exception {
        mvc.perform(post("/api/appointments").with(httpBasic("ana.user@test.local", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 2, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerWithoutCustomerRecordCannotBook() throws Exception {
        mvc.perform(post("/api/appointments").with(httpBasic("orphan@test.local", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                                 "endTime": "2026-01-14T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanCancelOnlyOwnAppointments() throws Exception {
        mvc.perform(post("/api/appointments/6/cancel").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/appointments/3/cancel").with(httpBasic("ana.user@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesUnmappedApiPaths() throws Exception {
        mvc.perform(get("/api/users").with(httpBasic("admin@test.local", PASSWORD)))
                .andExpect(status().isForbidden());
    }
}
