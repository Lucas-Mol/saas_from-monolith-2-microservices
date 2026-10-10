package dev.agenda.legacy.users;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class LegacyUserPrincipal extends org.springframework.security.core.userdetails.User {

    private final String fullName;
    private final String role;
    private final Long customerId;

    public LegacyUserPrincipal(User user) {
        super(user.getEmail(), user.getPasswordHash(), user.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.customerId = user.getCustomerId();
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(role);
    }

    public Long getCustomerId() {
        return customerId;
    }
}
