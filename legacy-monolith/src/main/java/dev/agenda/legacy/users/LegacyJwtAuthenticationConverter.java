package dev.agenda.legacy.users;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class LegacyJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final LegacyPrincipalFactory principalFactory;

    public LegacyJwtAuthenticationConverter(LegacyPrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new LegacyTokenAuthentication(principalFactory.create(jwt.getClaims()), jwt);
    }
}
