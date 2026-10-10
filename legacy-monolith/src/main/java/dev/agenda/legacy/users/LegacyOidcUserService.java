package dev.agenda.legacy.users;

import java.util.List;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import dev.agenda.legacy.config.LegacyProperties;

@Component
public class LegacyOidcUserService extends OidcUserService {

    private final LegacyPrincipalFactory principalFactory;
    private final String tenantId;

    public LegacyOidcUserService(LegacyPrincipalFactory principalFactory, LegacyProperties properties) {
        this.principalFactory = principalFactory;
        this.tenantId = properties.tenantId();
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        if (TokenClaims.narrowedToOneOrganization(userRequest.getAccessToken().getScopes())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied", "Organization-specific scopes are not accepted", null));
        }
        return toLegacyUser(super.loadUser(userRequest));
    }

    LegacyOidcUser toLegacyUser(OidcUser oidcUser) {
        if (!TokenClaims.tenants(oidcUser.getClaims()).equals(List.of(tenantId))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied", "Account does not belong to this instance", null));
        }
        return new LegacyOidcUser(principalFactory.create(oidcUser.getClaims()), oidcUser.getIdToken(),
                oidcUser.getUserInfo());
    }
}
