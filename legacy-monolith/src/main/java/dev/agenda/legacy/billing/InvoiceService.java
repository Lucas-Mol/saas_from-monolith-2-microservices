package dev.agenda.legacy.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.agenda.legacy.appointments.Appointment;
import dev.agenda.legacy.appointments.AppointmentRepository;
import dev.agenda.legacy.appointments.Professional;
import dev.agenda.legacy.appointments.ProfessionalRepository;
import dev.agenda.legacy.common.BusinessRuleException;
import dev.agenda.legacy.common.ConflictException;
import dev.agenda.legacy.common.NotFoundException;
import dev.agenda.legacy.customers.CustomerRepository;

@Service
public class InvoiceService {

    private static final DateTimeFormatter LINE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InvoiceRepository invoiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          AppointmentRepository appointmentRepository,
                          ProfessionalRepository professionalRepository,
                          CustomerRepository customerRepository,
                          Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.appointmentRepository = appointmentRepository;
        this.professionalRepository = professionalRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Invoice> list(InvoiceStatus status) {
        if (status != null) {
            return invoiceRepository.findByStatusOrderByIssuedAtDescIdDesc(status);
        }
        return invoiceRepository.findAllByOrderByIssuedAtDescIdDesc();
    }

    @Transactional
    public Invoice create(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new BusinessRuleException("Customer " + customerId + " does not exist");
        }
        List<Appointment> billable = appointmentRepository.findCompletedNotInvoiced(customerId);
        if (billable.isEmpty()) {
            throw new BusinessRuleException("Customer " + customerId + " has no completed appointments to invoice");
        }
        Map<Long, Professional> professionals = professionalRepository
                .findAllById(billable.stream().map(Appointment::getProfessionalId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Professional::getId, Function.identity()));

        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setIssuedAt(LocalDateTime.now(clock));
        BigDecimal total = BigDecimal.ZERO;
        for (Appointment appointment : billable) {
            Professional professional = professionals.get(appointment.getProfessionalId());
            InvoiceLine line = new InvoiceLine();
            line.setAppointmentId(appointment.getId());
            line.setDescription(professional.getSpecialty() + " session with " + professional.getFullName()
                    + " on " + appointment.getStartTime().format(LINE_DATE));
            line.setAmount(professional.getSessionPrice());
            invoice.getLines().add(line);
            total = total.add(professional.getSessionPrice());
        }
        invoice.setTotal(total);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice pay(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice " + id + " not found"));
        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new ConflictException("Only OPEN invoices can be paid (current: " + invoice.getStatus() + ")");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now(clock));
        return invoiceRepository.save(invoice);
    }
}
