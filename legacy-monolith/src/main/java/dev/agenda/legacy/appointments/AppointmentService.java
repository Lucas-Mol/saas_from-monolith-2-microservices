package dev.agenda.legacy.appointments;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.agenda.legacy.common.BusinessRuleException;
import dev.agenda.legacy.common.ConflictException;
import dev.agenda.legacy.common.ForbiddenException;
import dev.agenda.legacy.common.NotFoundException;
import dev.agenda.legacy.config.LegacyProperties;
import dev.agenda.legacy.customers.CustomerRepository;

@Service
public class AppointmentService {

    static final int SLOT_MINUTES = 60;

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final CustomerRepository customerRepository;
    private final LegacyProperties properties;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ProfessionalRepository professionalRepository,
                              CustomerRepository customerRepository,
                              LegacyProperties properties,
                              Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.professionalRepository = professionalRepository;
        this.customerRepository = customerRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Appointment> list(LocalDate from, LocalDate to, Long professionalId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start)) {
            throw new BusinessRuleException("'to' must not be before 'from'");
        }
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();
        if (professionalId != null) {
            return appointmentRepository
                    .findByProfessionalIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAscIdAsc(
                            professionalId, rangeStart, rangeEnd);
        }
        return appointmentRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAscIdAsc(
                rangeStart, rangeEnd);
    }

    @Transactional(readOnly = true)
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment " + id + " not found"));
    }

    private Appointment findByIdForUpdate(Long id) {
        return appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Appointment " + id + " not found"));
    }

    @Transactional
    public Appointment schedule(AppointmentRequest request, Long actingCustomerId) {
        if (actingCustomerId != null && !actingCustomerId.equals(request.getCustomerId())) {
            throw new ForbiddenException("Customers can only book appointments for themselves");
        }
        if (!customerRepository.existsById(request.getCustomerId())) {
            throw new BusinessRuleException("Customer " + request.getCustomerId() + " does not exist");
        }
        Professional professional = professionalRepository.findByIdForUpdate(request.getProfessionalId())
                .filter(Professional::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "Professional " + request.getProfessionalId() + " does not exist or is inactive"));

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("End time must be after start time");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessRuleException("Appointment must start and end on the same day");
        }
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Appointment must start in the future");
        }
        if (start.toLocalTime().isBefore(professional.getWorkStart())
                || end.toLocalTime().isAfter(professional.getWorkEnd())) {
            throw new BusinessRuleException("Appointment is outside the professional's working hours ("
                    + professional.getWorkStart() + "-" + professional.getWorkEnd() + ")");
        }
        if (!appointmentRepository.findActiveOverlapping(professional.getId(), start, end).isEmpty()) {
            throw new ConflictException("Professional already has an appointment in this time range");
        }

        Appointment appointment = new Appointment();
        appointment.setCustomerId(request.getCustomerId());
        appointment.setProfessionalId(professional.getId());
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.getNotes());
        appointment.setCreatedAt(LocalDateTime.now(clock));
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment confirm(Long id) {
        Appointment appointment = findByIdForUpdate(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be confirmed (current: "
                    + appointment.getStatus() + ")");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment complete(Long id) {
        Appointment appointment = findByIdForUpdate(id);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only CONFIRMED appointments can be completed (current: "
                    + appointment.getStatus() + ")");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancel(Long id, String reason, Long actingCustomerId) {
        Appointment appointment = findByIdForUpdate(id);
        if (actingCustomerId != null && !actingCustomerId.equals(appointment.getCustomerId())) {
            throw new ForbiddenException("Customers can only cancel their own appointments");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only SCHEDULED or CONFIRMED appointments can be cancelled (current: "
                    + appointment.getStatus() + ")");
        }
        int minHours = properties.cancellationMinHours();
        if (LocalDateTime.now(clock).plusHours(minHours).isAfter(appointment.getStartTime())) {
            throw new BusinessRuleException("Appointments can only be cancelled at least " + minHours
                    + " hours in advance");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> availability(Long professionalId, LocalDate date) {
        Professional professional = professionalRepository.findById(professionalId)
                .filter(Professional::isActive)
                .orElseThrow(() -> new NotFoundException("Professional " + professionalId + " not found"));
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dayStart = date.atTime(professional.getWorkStart());
        LocalDateTime dayEnd = date.atTime(professional.getWorkEnd());
        List<Appointment> busy = appointmentRepository.findActiveOverlapping(professionalId, dayStart, dayEnd);

        List<TimeSlot> slots = new ArrayList<>();
        LocalTime cursor = professional.getWorkStart();
        while (!cursor.plusMinutes(SLOT_MINUTES).isAfter(professional.getWorkEnd())
                && cursor.plusMinutes(SLOT_MINUTES).isAfter(cursor)) {
            LocalDateTime slotStart = date.atTime(cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(SLOT_MINUTES);
            boolean free = busy.stream().noneMatch(a -> a.getStartTime().isBefore(slotEnd)
                    && a.getEndTime().isAfter(slotStart));
            if (free && slotStart.isAfter(now)) {
                slots.add(new TimeSlot(slotStart, slotEnd));
            }
            cursor = cursor.plusMinutes(SLOT_MINUTES);
        }
        return slots;
    }
}
