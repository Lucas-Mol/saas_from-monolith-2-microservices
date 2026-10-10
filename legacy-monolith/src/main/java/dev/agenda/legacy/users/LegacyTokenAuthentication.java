package dev.agenda.legacy.users;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

public class LegacyTokenAuthentication extends AbstractAuthenticationToken {

    private final LegacyUserPrincipal principal;
    private final transient Jwt token;

    public LegacyTokenAuthentication(LegacyUserPrincipal principal, Jwt token) {
        super(LegacyPrincipalFactory.authorities(principal));
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public LegacyUserPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }
}
