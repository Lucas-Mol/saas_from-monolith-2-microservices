package dev.agenda.legacy.appointments;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.agenda.legacy.common.ForbiddenException;
import dev.agenda.legacy.users.LegacyUserPrincipal;

@RestController
@RequestMapping("/api")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public List<Appointment> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) Long professionalId) {
        return appointmentService.list(from, to, professionalId);
    }

    @PostMapping("/appointments")
    public ResponseEntity<Appointment> schedule(@Valid @RequestBody AppointmentRequest request,
                                                @AuthenticationPrincipal LegacyUserPrincipal user) {
        Appointment appointment = appointmentService.schedule(request, actingCustomerId(user));
        return ResponseEntity.created(URI.create("/api/appointments/" + appointment.getId())).body(appointment);
    }

    @PostMapping("/appointments/{id}/confirm")
    public Appointment confirm(@PathVariable Long id) {
        return appointmentService.confirm(id);
    }

    @PostMapping("/appointments/{id}/cancel")
    public Appointment cancel(@PathVariable Long id,
                              @Valid @RequestBody(required = false) CancelRequest request,
                              @AuthenticationPrincipal LegacyUserPrincipal user) {
        String reason = request != null ? request.getReason() : null;
        return appointmentService.cancel(id, reason, actingCustomerId(user));
    }

    @PostMapping("/appointments/{id}/complete")
    public Appointment complete(@PathVariable Long id) {
        return appointmentService.complete(id);
    }

    @GetMapping("/availability")
    public List<TimeSlot> availability(@RequestParam Long professionalId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.availability(professionalId, date);
    }

    private static Long actingCustomerId(LegacyUserPrincipal user) {
        if (!user.isCustomer()) {
            return null;
        }
        if (user.getCustomerId() == null) {
            throw new ForbiddenException("User is not linked to a customer record");
        }
        return user.getCustomerId();
    }
}
