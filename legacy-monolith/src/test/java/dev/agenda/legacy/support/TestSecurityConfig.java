package dev.agenda.legacy.support;

import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import dev.agenda.legacy.config.SecurityConfig;

@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return TestJwts.decoder();
    }

    @Bean
    @Primary
    ClientRegistrationRepository testClientRegistrations() {
        return new InMemoryClientRegistrationRepository(registration());
    }

    public static ClientRegistration registration() {
        String realm = TestJwts.ISSUER;
        return ClientRegistration.withRegistrationId(SecurityConfig.REGISTRATION_ID)
                .clientId("backoffice-test")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .issuerUri(realm)
                .authorizationUri(realm + "/protocol/openid-connect/auth")
                .tokenUri(realm + "/protocol/openid-connect/token")
                .jwkSetUri(TestIdentityProvider.jwkSetUri())
                .userInfoUri(realm + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", realm + "/protocol/openid-connect/logout"))
                .build();
    }
}
