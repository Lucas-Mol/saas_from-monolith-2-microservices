package dev.agenda.legacy.billing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByIssuedAtDescIdDesc();

    List<Invoice> findByStatusOrderByIssuedAtDescIdDesc(InvoiceStatus status);
}
