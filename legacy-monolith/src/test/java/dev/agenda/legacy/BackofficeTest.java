package dev.agenda.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import dev.agenda.legacy.appointments.AppointmentRepository;
import dev.agenda.legacy.appointments.AppointmentStatus;
import dev.agenda.legacy.billing.InvoiceRepository;
import dev.agenda.legacy.billing.InvoiceStatus;
import dev.agenda.legacy.customers.Customer;
import dev.agenda.legacy.customers.CustomerRepository;
import dev.agenda.legacy.support.IntegrationTest;

@IntegrationTest
class BackofficeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UserDetails admin() {
        return userDetailsService.loadUserByUsername("admin@test.local");
    }

    private UserDetails staff() {
        return userDetailsService.loadUserByUsername("staff@test.local");
    }

    @Test
    void redirectsAnonymousUsersToLogin() throws Exception {
        mvc.perform(get("/backoffice/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void rendersLoginPageWithInstanceName() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Test Instance")));
    }

    @Test
    void logsInWithSessionAndRedirectsToBackoffice() throws Exception {
        mvc.perform(formLogin("/login").user("staff@test.local").password("test-password"))
                .andExpect(redirectedUrl("/backoffice"));
    }

    @Test
    void rejectsInvalidLogin() throws Exception {
        mvc.perform(formLogin("/login").user("staff@test.local").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void showsTodaysAgenda() throws Exception {
        mvc.perform(get("/backoffice/appointments").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.containsString("Ana Test"),
                        Matchers.containsString("Dr. Paula Test"),
                        Matchers.containsString("Test Staff"))));
    }

    @Test
    void customersCannotUseBackoffice() throws Exception {
        UserDetails customer = userDetailsService.loadUserByUsername("ana.user@test.local");
        mvc.perform(get("/backoffice/appointments").with(user(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotOpenInvoices() throws Exception {
        mvc.perform(get("/backoffice/invoices").with(user(staff())))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresCsrfTokenOnForms() throws Exception {
        mvc.perform(post("/backoffice/appointments/2/confirm").param("date", "2026-01-12").with(user(staff())))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmsAppointmentFromBackoffice() throws Exception {
        mvc.perform(post("/backoffice/appointments/2/confirm").param("date", "2026-01-12")
                        .with(user(staff())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/appointments?date=2026-01-12"));

        assertThat(appointmentRepository.findById(2L)).get()
                .extracting("status").isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void showsBusinessRuleErrorsAsFlashMessages() throws Exception {
        mvc.perform(post("/backoffice/appointments/2/cancel").param("date", "2026-01-12")
                        .with(user(staff())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/appointments?date=2026-01-12"))
                .andExpect(flash().attribute("error", "Appointments can only be cancelled at least 24 hours in advance"));

        assertThat(appointmentRepository.findById(2L)).get()
                .extracting("status").isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void schedulesAppointmentFromForm() throws Exception {
        mvc.perform(post("/backoffice/appointments")
                        .param("customerId", "3").param("professionalId", "1")
                        .param("startTime", "2026-01-14T09:00").param("endTime", "2026-01-14T10:00")
                        .param("notes", "")
                        .with(user(staff())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/appointments?date=2026-01-14"))
                .andExpect(flash().attribute("message", "Appointment scheduled."));

        assertThat(appointmentRepository.findById(8L)).get().satisfies(appointment -> {
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            assertThat(appointment.getNotes()).isNull();
        });
    }

    @Test
    void storesEmptyOptionalCustomerFieldsAsNull() throws Exception {
        mvc.perform(post("/backoffice/customers")
                        .param("fullName", "  Eva Form  ").param("email", "").param("phone", "")
                        .param("birthDate", "").param("notes", "")
                        .with(user(staff())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/customers"))
                .andExpect(flash().attribute("message", "Customer created."));

        Customer created = customerRepository.findById(4L).orElseThrow();
        assertThat(created.getFullName()).isEqualTo("Eva Form");
        assertThat(created.getEmail()).isNull();
        assertThat(created.getPhone()).isNull();
        assertThat(created.getNotes()).isNull();
    }

    @Test
    void issuesAndPaysInvoice() throws Exception {
        mvc.perform(post("/backoffice/invoices").param("customerId", "2").with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/invoices"))
                .andExpect(flash().attribute("message", "Invoice issued."));
        assertThat(invoiceRepository.findById(2L)).get().extracting("status").isEqualTo(InvoiceStatus.OPEN);

        mvc.perform(post("/backoffice/invoices/2/pay").with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/invoices"))
                .andExpect(flash().attribute("message", "Invoice #2 marked as paid."));
        assertThat(invoiceRepository.findById(2L)).get().extracting("status").isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void showsErrorWhenThereIsNothingToInvoice() throws Exception {
        mvc.perform(post("/backoffice/invoices").param("customerId", "1").with(user(admin())).with(csrf()))
                .andExpect(redirectedUrl("/backoffice/invoices"))
                .andExpect(flash().attribute("error", "Customer 1 has no completed appointments to invoice"));
    }
}
