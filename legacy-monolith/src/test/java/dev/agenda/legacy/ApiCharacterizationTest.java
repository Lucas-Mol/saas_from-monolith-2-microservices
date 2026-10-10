package dev.agenda.legacy;

import static dev.agenda.legacy.support.GoldenMaster.assertMatches;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.agenda.legacy.support.IntegrationTest;
import dev.agenda.legacy.support.TestJwts;

@IntegrationTest
class ApiCharacterizationTest {

    @Autowired
    private TestRestTemplate rest;

    private ResponseEntity<String> get(String user, String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(user)), String.class);
    }

    private ResponseEntity<String> post(String user, String path, String json) {
        HttpHeaders headers = bearer(user);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(json, headers),
                String.class);
    }

    private ResponseEntity<String> put(String user, String path, String json) {
        HttpHeaders headers = bearer(user);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(json, headers),
                String.class);
    }

    private static HttpHeaders bearer(String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.forUser(user));
        return headers;
    }

    @Nested
    class Customers {

        @Test
        void listsCustomersOrderedByName() {
            assertMatches("customers-list", get("staff@test.local", "/api/customers"));
        }

        @Test
        void getsCustomerById() {
            assertMatches("customers-get", get("staff@test.local", "/api/customers/2"));
        }

        @Test
        void returns404ForUnknownCustomer() {
            assertMatches("customers-get-unknown", get("staff@test.local", "/api/customers/999"));
        }

        @Test
        void createsCustomer() {
            assertMatches("customers-create", post("staff@test.local", "/api/customers", """
                    {"fullName": "Dora New", "email": "dora@test.local", "phone": "+55 11 93333-0000",
                     "birthDate": "1999-05-05", "notes": "Walk-in"}
                    """));
        }

        @Test
        void rejectsInvalidCustomer() {
            assertMatches("customers-create-invalid", post("staff@test.local", "/api/customers", """
                    {"fullName": " ", "email": "not-an-email"}
                    """));
        }

        @Test
        void updatesCustomer() {
            assertMatches("customers-update", put("admin@test.local", "/api/customers/3", """
                    {"fullName": "Cris Updated", "email": "cris.updated@test.local"}
                    """));
        }
    }

    @Nested
    class Professionals {

        @Test
        void listsActiveProfessionals() {
            assertMatches("professionals-list", get("ana.user@test.local", "/api/professionals"));
        }
    }

    @Nested
    class Appointments {

        @Test
        void listsTodayByDefault() {
            assertMatches("appointments-list-today", get("staff@test.local", "/api/appointments"));
        }

        @Test
        void listsRangeFilteredByProfessional() {
            assertMatches("appointments-list-range-professional",
                    get("staff@test.local", "/api/appointments?from=2026-01-05&to=2026-01-15&professionalId=1"));
        }

        @Test
        void rejectsInvertedRange() {
            assertMatches("appointments-list-inverted-range",
                    get("staff@test.local", "/api/appointments?from=2026-01-15&to=2026-01-05"));
        }

        @Test
        void schedulesAppointment() {
            assertMatches("appointments-schedule", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-12T11:00:00",
                     "endTime": "2026-01-12T12:00:00", "notes": "Back pain"}
                    """));
        }

        @Test
        void rejectsOverlappingAppointment() {
            assertMatches("appointments-schedule-overlap", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-12T09:30:00",
                     "endTime": "2026-01-12T10:30:00"}
                    """));
        }

        @Test
        void allowsBookingOverCancelledAppointment() {
            assertMatches("appointments-schedule-over-cancelled", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 2, "startTime": "2026-01-13T10:00:00",
                     "endTime": "2026-01-13T11:00:00"}
                    """));
        }

        @Test
        void rejectsAppointmentOutsideWorkingHours() {
            assertMatches("appointments-schedule-outside-hours", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 2, "startTime": "2026-01-12T17:30:00",
                     "endTime": "2026-01-12T18:30:00"}
                    """));
        }

        @Test
        void rejectsAppointmentInThePast() {
            assertMatches("appointments-schedule-past", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-12T07:00:00",
                     "endTime": "2026-01-12T08:00:00"}
                    """));
        }

        @Test
        void rejectsInactiveProfessional() {
            assertMatches("appointments-schedule-inactive-professional", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 3, "startTime": "2026-01-14T09:00:00",
                     "endTime": "2026-01-14T10:00:00"}
                    """));
        }

        @Test
        void rejectsMissingFields() {
            assertMatches("appointments-schedule-missing-fields", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3}
                    """));
        }

        @Test
        void allowsBookingExactlyAtWorkStart() {
            assertMatches("appointments-schedule-at-work-start", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-14T08:00:00",
                     "endTime": "2026-01-14T09:00:00"}
                    """));
        }

        @Test
        void allowsBookingEndingExactlyAtWorkEnd() {
            assertMatches("appointments-schedule-at-work-end", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-14T16:00:00",
                     "endTime": "2026-01-14T17:00:00"}
                    """));
        }

        @Test
        void allowsBookingRightAfterAnotherAppointment() {
            assertMatches("appointments-schedule-adjacent", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 2, "startTime": "2026-01-12T15:00:00",
                     "endTime": "2026-01-12T16:00:00"}
                    """));
        }

        @Test
        void rejectsEndEqualToStart() {
            assertMatches("appointments-schedule-zero-duration", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                     "endTime": "2026-01-14T09:00:00"}
                    """));
        }

        @Test
        void rejectsAppointmentSpanningTwoDays() {
            assertMatches("appointments-schedule-two-days", post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-14T16:00:00",
                     "endTime": "2026-01-15T09:00:00"}
                    """));
        }

        @Test
        void rejectsUnknownCustomer() {
            assertMatches("appointments-schedule-unknown-customer", post("staff@test.local", "/api/appointments", """
                    {"customerId": 999, "professionalId": 1, "startTime": "2026-01-14T09:00:00",
                     "endTime": "2026-01-14T10:00:00"}
                    """));
        }

        @Test
        void confirmsScheduledAppointment() {
            assertMatches("appointments-confirm", post("staff@test.local", "/api/appointments/2/confirm", null));
        }

        @Test
        void rejectsConfirmingConfirmedAppointment() {
            assertMatches("appointments-confirm-invalid-state",
                    post("staff@test.local", "/api/appointments/1/confirm", null));
        }

        @Test
        void completesConfirmedAppointment() {
            assertMatches("appointments-complete", post("staff@test.local", "/api/appointments/1/complete", null));
        }

        @Test
        void rejectsCompletingScheduledAppointment() {
            assertMatches("appointments-complete-invalid-state",
                    post("staff@test.local", "/api/appointments/2/complete", null));
        }

        @Test
        void cancelsAppointmentInAdvance() {
            assertMatches("appointments-cancel", post("staff@test.local", "/api/appointments/6/cancel", """
                    {"reason": "Rescheduling"}
                    """));
        }

        @Test
        void allowsCancellationExactlyAtTheMinimumNotice() {
            post("staff@test.local", "/api/appointments", """
                    {"customerId": 3, "professionalId": 1, "startTime": "2026-01-13T08:00:00",
                     "endTime": "2026-01-13T09:00:00"}
                    """);
            assertMatches("appointments-cancel-at-minimum-notice",
                    post("staff@test.local", "/api/appointments/8/cancel", null));
        }

        @Test
        void rejectsCancellingCompletedAppointment() {
            assertMatches("appointments-cancel-completed", post("staff@test.local", "/api/appointments/4/cancel", null));
        }

        @Test
        void rejectsCancellingCancelledAppointment() {
            assertMatches("appointments-cancel-cancelled", post("staff@test.local", "/api/appointments/7/cancel", null));
        }

        @Test
        void rejectsLateCancellation() {
            assertMatches("appointments-cancel-too-late",
                    post("staff@test.local", "/api/appointments/2/cancel", null));
        }

        @Test
        void returns404ForUnknownAppointment() {
            assertMatches("appointments-confirm-unknown",
                    post("staff@test.local", "/api/appointments/999/confirm", null));
        }

        @Test
        void listsAvailability() {
            assertMatches("availability",
                    get("ana.user@test.local", "/api/availability?professionalId=1&date=2026-01-12"));
        }

        @Test
        void listsAvailabilityAroundCancelledAppointment() {
            assertMatches("availability-with-cancelled",
                    get("staff@test.local", "/api/availability?professionalId=2&date=2026-01-13"));
        }

        @Test
        void returns404ForAvailabilityOfInactiveProfessional() {
            assertMatches("availability-inactive-professional",
                    get("staff@test.local", "/api/availability?professionalId=3&date=2026-01-14"));
        }

        @Test
        void rejectsAvailabilityWithoutDate() {
            assertMatches("availability-missing-date", get("staff@test.local", "/api/availability?professionalId=1"));
        }
    }

    @Nested
    class Invoices {

        @Test
        void listsInvoices() {
            assertMatches("invoices-list", get("admin@test.local", "/api/invoices"));
        }

        @Test
        void createsInvoiceForCompletedAppointments() {
            assertMatches("invoices-create", post("admin@test.local", "/api/invoices", """
                    {"customerId": 2}
                    """));
        }

        @Test
        void rejectsInvoiceWithoutBillableAppointments() {
            assertMatches("invoices-create-nothing-to-bill", post("admin@test.local", "/api/invoices", """
                    {"customerId": 1}
                    """));
        }

        @Test
        void paysOpenInvoice() {
            post("admin@test.local", "/api/invoices", """
                    {"customerId": 2}
                    """);
            assertMatches("invoices-pay", post("admin@test.local", "/api/invoices/2/pay", null));
        }

        @Test
        void rejectsPayingPaidInvoice() {
            assertMatches("invoices-pay-already-paid", post("admin@test.local", "/api/invoices/1/pay", null));
        }
    }
}
