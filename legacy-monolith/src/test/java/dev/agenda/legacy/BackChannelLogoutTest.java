package dev.agenda.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jwt.JWTClaimsSet;

import dev.agenda.legacy.support.IntegrationTest;
import dev.agenda.legacy.support.TestJwts;
import dev.agenda.legacy.users.LegacyOidcUser;
import dev.agenda.legacy.users.LegacyUserPrincipal;

@IntegrationTest
class BackChannelLogoutTest {

    private static final WireMockServer SELF = new WireMockServer(options().dynamicPort());

    static {
        SELF.start();
    }

    @AfterAll
    static void stop() {
        SELF.stop();
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OidcSessionRegistry sessions;

    @Test
    void endsTheMatchingBackofficeSessionUsingTheInstanceCookieName() throws Exception {
        SELF.stubFor(WireMock.post("/logout/connect/back-channel/keycloak").willReturn(aResponse().withStatus(200)));
        String sid = UUID.randomUUID().toString();
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
                .issuer(TestJwts.ISSUER).subject("staff-subject").audience(List.of("backoffice-test"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("sid", sid).build();
        LegacyOidcUser user = new LegacyOidcUser(new LegacyUserPrincipal("staff@test.local", "Test Staff", "STAFF", null),
                idToken, null);
        sessions.saveSessionInformation(new OidcSessionInformation("session-123", Map.of("X-CSRF-TOKEN", "csrf-123"), user));

        mvc.perform(post("/logout/connect/back-channel/keycloak")
                        .with(request -> {
                            request.setServerPort(SELF.port());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logout_token", logoutToken(sid)))
                .andExpect(status().isOk());

        SELF.verify(postRequestedFor(urlEqualTo("/logout/connect/back-channel/keycloak"))
                .withHeader("Cookie", containing("TEST_SESSION=session-123"))
                .withHeader("X-CSRF-TOKEN", containing("csrf-123")));
    }

    @Test
    void rejectsLogoutTokensNotSignedByTheIdentityProvider() throws Exception {
        mvc.perform(post("/logout/connect/back-channel/keycloak")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logout_token", TestJwts.token().build()))
                .andExpect(status().isBadRequest());
    }

    private static String logoutToken(String sid) {
        return TestJwts.sign(new JWTClaimsSet.Builder()
                .issuer(TestJwts.ISSUER)
                .subject("staff-subject")
                .audience("backoffice-test")
                .issueTime(Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .claim("sid", sid)
                .claim("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of()))
                .build());
    }
}
