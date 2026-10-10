package dev.agenda.legacy.billing;

import jakarta.validation.constraints.NotNull;

public class InvoiceRequest {

    @NotNull
    private Long customerId;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}
