package dev.agenda.legacy.backoffice;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.agenda.legacy.appointments.AppointmentRequest;
import dev.agenda.legacy.appointments.AppointmentService;
import dev.agenda.legacy.appointments.Professional;
import dev.agenda.legacy.appointments.ProfessionalRepository;
import dev.agenda.legacy.billing.InvoiceService;
import dev.agenda.legacy.common.BusinessRuleException;
import dev.agenda.legacy.common.ConflictException;
import dev.agenda.legacy.common.NotFoundException;
import dev.agenda.legacy.config.LegacyProperties;
import dev.agenda.legacy.customers.Customer;
import dev.agenda.legacy.customers.CustomerRequest;
import dev.agenda.legacy.customers.CustomerService;

@Controller
public class BackofficeController {

    private final AppointmentService appointmentService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final ProfessionalRepository professionalRepository;
    private final LegacyProperties properties;
    private final Clock clock;

    public BackofficeController(AppointmentService appointmentService,
                                CustomerService customerService,
                                InvoiceService invoiceService,
                                ProfessionalRepository professionalRepository,
                                LegacyProperties properties,
                                Clock clock) {
        this.appointmentService = appointmentService;
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.professionalRepository = professionalRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @InitBinder
    public void trimEmptyStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("instanceName")
    public String instanceName() {
        return properties.instanceName();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/backoffice"})
    public String home() {
        return "redirect:/backoffice/appointments";
    }

    @GetMapping("/backoffice/appointments")
    public String appointments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        model.addAttribute("date", day);
        model.addAttribute("appointments", appointmentService.list(day, day, null));
        model.addAttribute("customers", customersById());
        model.addAttribute("professionals", professionalsById());
        return "appointments";
    }

    @GetMapping("/backoffice/appointments/new")
    public String newAppointment(Model model) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("professionals", professionalRepository.findByActiveTrueOrderByFullNameAsc());
        return "appointment-form";
    }

    @PostMapping("/backoffice/appointments")
    public String createAppointment(@Valid @ModelAttribute AppointmentRequest request, BindingResult bindingResult,
                                    RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("error", "Please fill in every required field.");
            return "redirect:/backoffice/appointments/new";
        }
        try {
            appointmentService.schedule(request, null);
        } catch (BusinessRuleException | ConflictException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/backoffice/appointments/new";
        }
        redirect.addFlashAttribute("message", "Appointment scheduled.");
        redirect.addAttribute("date", request.getStartTime().toLocalDate().toString());
        return "redirect:/backoffice/appointments";
    }

    @PostMapping("/backoffice/appointments/{id}/{action:confirm|cancel|complete}")
    public String changeAppointmentStatus(@PathVariable Long id, @PathVariable String action,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          RedirectAttributes redirect) {
        try {
            switch (action) {
                case "confirm" -> appointmentService.confirm(id);
                case "cancel" -> appointmentService.cancel(id, "Cancelled by the front desk", null);
                case "complete" -> appointmentService.complete(id);
                default -> throw new IllegalStateException("Unexpected action " + action);
            }
            redirect.addFlashAttribute("message", "Appointment #" + id + " updated.");
        } catch (NotFoundException | BusinessRuleException | ConflictException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        redirect.addAttribute("date", date.toString());
        return "redirect:/backoffice/appointments";
    }

    @GetMapping("/backoffice/customers")
    public String customers(Model model) {
        model.addAttribute("customers", customerService.findAll());
        return "customers";
    }

    @PostMapping("/backoffice/customers")
    public String createCustomer(@Valid @ModelAttribute CustomerRequest request, BindingResult bindingResult,
                                 RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            redirect.addFlashAttribute("error", "Invalid customer data: " + bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
            return "redirect:/backoffice/customers";
        }
        customerService.create(request);
        redirect.addFlashAttribute("message", "Customer created.");
        return "redirect:/backoffice/customers";
    }

    @GetMapping("/backoffice/invoices")
    public String invoices(Model model) {
        model.addAttribute("invoices", invoiceService.list(null));
        model.addAttribute("customers", customersById());
        model.addAttribute("customerList", customerService.findAll());
        return "invoices";
    }

    @PostMapping("/backoffice/invoices")
    public String createInvoice(@RequestParam Long customerId, RedirectAttributes redirect) {
        try {
            invoiceService.create(customerId);
            redirect.addFlashAttribute("message", "Invoice issued.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error", "Some appointments were invoiced at the same time by someone else. "
                    + "Please review the invoices and try again.");
        }
        return "redirect:/backoffice/invoices";
    }

    @PostMapping("/backoffice/invoices/{id}/pay")
    public String payInvoice(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            invoiceService.pay(id);
            redirect.addFlashAttribute("message", "Invoice #" + id + " marked as paid.");
        } catch (NotFoundException | ConflictException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/backoffice/invoices";
    }

    private Map<Long, Customer> customersById() {
        return customerService.findAll().stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private Map<Long, Professional> professionalsById() {
        return professionalRepository.findAll().stream()
                .collect(Collectors.toMap(Professional::getId, Function.identity()));
    }
}
