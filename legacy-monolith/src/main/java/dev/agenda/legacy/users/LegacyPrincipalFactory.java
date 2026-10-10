package dev.agenda.legacy.users;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyPrincipalFactory {

    private static final List<String> LEGACY_ROLES = List.of("ADMIN", "STAFF", "CUSTOMER");

    private final UserRepository userRepository;

    public LegacyPrincipalFactory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LegacyUserPrincipal create(Map<String, Object> claims) {
        String email = text(claims, "email", text(claims, "preferred_username", null));
        String fullName = text(claims, "name", email);
        List<String> roles = TokenClaims.realmRoles(claims);
        String role = LEGACY_ROLES.stream().filter(roles::contains).findFirst().orElse(null);
        Long customerId = "CUSTOMER".equals(role) && email != null && TokenClaims.emailVerified(claims)
                ? userRepository.findByEmailIgnoreCase(email).map(User::getCustomerId).orElse(null)
                : null;
        return new LegacyUserPrincipal(email, fullName, role, customerId);
    }

    public static List<GrantedAuthority> authorities(LegacyUserPrincipal principal) {
        return principal.getRole() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));
    }

    private static String text(Map<String, Object> claims, String name, String fallback) {
        return claims.get(name) instanceof String value && !value.isBlank() ? value : fallback;
    }
}
