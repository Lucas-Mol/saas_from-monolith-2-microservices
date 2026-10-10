package dev.agenda.legacy.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import dev.agenda.legacy.config.LegacyProperties;
import dev.agenda.legacy.support.TestJwts;
import dev.agenda.legacy.support.TestOidcUsers;

class LegacyOidcUserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LegacyOidcUserService service = new LegacyOidcUserService(new LegacyPrincipalFactory(users),
            new LegacyProperties("Test Instance", 24, TestJwts.TENANT));

    @Test
    void mapsTenantUserToTheLegacyModel() {
        LegacyOidcUser user = service.toLegacyUser(oidcUser("staff@test.local", "STAFF", List.of(TestJwts.TENANT)));

        assertThat(user.getLegacyUser().getEmail()).isEqualTo("staff@test.local");
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_STAFF");
    }

    @Test
    void linksCustomersToTheirCustomerRecordByEmail() {
        User linked = mock(User.class);
        when(linked.getCustomerId()).thenReturn(7L);
        when(users.findByEmailIgnoreCase("ana.user@test.local")).thenReturn(Optional.of(linked));

        LegacyOidcUser user = service.toLegacyUser(oidcUser("ana.user@test.local", "CUSTOMER", List.of(TestJwts.TENANT)));

        assertThat(user.getLegacyUser().isCustomer()).isTrue();
        assertThat(user.getLegacyUser().getCustomerId()).isEqualTo(7L);
    }

    @Test
    void rejectsUsersOfOtherTenantsOrWithoutTenant() {
        for (Object tenant : Arrays.asList(List.of("other-tenant"), List.of(TestJwts.TENANT, "other-tenant"),
                null)) {
            assertThatThrownBy(() -> service.toLegacyUser(oidcUser("staff@test.local", "STAFF", tenant)))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Test
    void doesNotLinkCustomersWithUnverifiedEmail() {
        User linked = mock(User.class);
        when(linked.getCustomerId()).thenReturn(7L);
        when(users.findByEmailIgnoreCase("ana.user@test.local")).thenReturn(Optional.of(linked));
        OidcIdToken token = OidcIdToken.withTokenValue("id").claims(c -> c.putAll(
                TestOidcUsers.idToken("ana.user@test.local", "CUSTOMER", List.of(TestJwts.TENANT)).getClaims()))
                .claim("email_verified", false).build();

        LegacyOidcUser user = service.toLegacyUser(new DefaultOidcUser(List.of(), token));

        assertThat(user.getLegacyUser().getCustomerId()).isNull();
    }

    private static DefaultOidcUser oidcUser(String email, String role, Object tenantClaim) {
        return new DefaultOidcUser(List.of(), TestOidcUsers.idToken(email, role, tenantClaim));
    }
}
