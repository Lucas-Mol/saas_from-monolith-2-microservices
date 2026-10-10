package dev.agenda.legacy.users;

import java.io.Serializable;
import java.security.Principal;

public class LegacyUserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

    private final String email;
    private final String fullName;
    private final String role;
    private final Long customerId;

    public LegacyUserPrincipal(String email, String fullName, String role, Long customerId) {
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.customerId = customerId;
    }

    @Override
    public String getName() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(role);
    }

    public Long getCustomerId() {
        return customerId;
    }
}
