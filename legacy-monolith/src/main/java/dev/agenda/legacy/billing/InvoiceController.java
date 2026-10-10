package dev.agenda.legacy.billing;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<Invoice> list(@RequestParam(required = false) InvoiceStatus status) {
        return invoiceService.list(status);
    }

    @PostMapping
    public ResponseEntity<Invoice> create(@Valid @RequestBody InvoiceRequest request) {
        Invoice invoice = invoiceService.create(request.getCustomerId());
        return ResponseEntity.created(URI.create("/api/invoices/" + invoice.getId())).body(invoice);
    }

    @PostMapping("/{id}/pay")
    public Invoice pay(@PathVariable Long id) {
        return invoiceService.pay(id);
    }
}
