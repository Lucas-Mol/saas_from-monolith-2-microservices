package dev.agenda.legacy.users;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public class LegacyOidcUser extends DefaultOidcUser {

    private static final long serialVersionUID = 1L;

    private final LegacyUserPrincipal legacyUser;

    public LegacyOidcUser(LegacyUserPrincipal legacyUser, OidcIdToken idToken, OidcUserInfo userInfo) {
        super(LegacyPrincipalFactory.authorities(legacyUser), idToken, userInfo);
        this.legacyUser = legacyUser;
    }

    public LegacyUserPrincipal getLegacyUser() {
        return legacyUser;
    }

    public String getFullName() {
        return legacyUser.getFullName();
    }
}
