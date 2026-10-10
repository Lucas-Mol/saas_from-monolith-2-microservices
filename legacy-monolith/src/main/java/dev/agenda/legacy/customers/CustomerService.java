package dev.agenda.legacy.customers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.agenda.legacy.common.NotFoundException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public CustomerService(CustomerRepository customerRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        Customer customer = new Customer();
        copy(request, customer);
        customer.setCreatedAt(LocalDateTime.now(clock));
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        copy(request, customer);
        return customerRepository.save(customer);
    }

    private void copy(CustomerRequest request, Customer customer) {
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setBirthDate(request.getBirthDate());
        customer.setNotes(request.getNotes());
    }
}
