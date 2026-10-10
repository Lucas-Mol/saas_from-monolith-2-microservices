package dev.agenda.legacy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcBackChannelLogoutHandler;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.session.InMemoryOidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.util.function.SingletonSupplier;

import dev.agenda.legacy.users.LegacyJwtAuthenticationConverter;
import dev.agenda.legacy.users.LegacyOidcUserService;

@Configuration
public class SecurityConfig {

    public static final String REGISTRATION_ID = "keycloak";

    private static final String ADMIN = "ADMIN";
    private static final String STAFF = "STAFF";
    private static final String CUSTOMER = "CUSTOMER";

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http, LegacyJwtAuthenticationConverter tokenConverter)
            throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasAnyRole(ADMIN, STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/appointments").hasAnyRole(ADMIN, STAFF, CUSTOMER)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/cancel").hasAnyRole(ADMIN, STAFF, CUSTOMER)
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/confirm", "/api/appointments/*/complete")
                            .hasAnyRole(ADMIN, STAFF)
                        .requestMatchers(HttpMethod.GET, "/api/availability", "/api/professionals").authenticated()
                        .requestMatchers("/api/customers", "/api/customers/**").hasAnyRole(ADMIN, STAFF)
                        .requestMatchers("/api/invoices", "/api/invoices/**").hasRole(ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(tokenConverter))
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new RejectFormContentTypeFilter(), BearerTokenAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain backofficeSecurity(HttpSecurity http, ClientRegistrationRepository registrations,
                                                  LegacyOidcUserService oidcUserService,
                                                  OidcSessionRegistry sessionRegistry,
                                                  @Value("${server.servlet.session.cookie.name:JSESSIONID}")
                                                  String sessionCookieName) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequests =
                new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        authorizationRequests.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        OidcClientInitiatedLogoutSuccessHandler logoutSuccess = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        logoutSuccess.setPostLogoutRedirectUri("{baseUrl}/login?logout");

        OidcBackChannelLogoutHandler backChannelLogout = new OidcBackChannelLogoutHandler(sessionRegistry);
        backChannelLogout.setSessionCookieName(sessionCookieName);

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/backoffice/invoices", "/backoffice/invoices/**").hasRole(ADMIN)
                        .requestMatchers("/", "/backoffice", "/backoffice/**").hasAnyRole(ADMIN, STAFF)
                        .anyRequest().denyAll())
                .oauth2Login(login -> login
                        .loginPage("/oauth2/authorization/" + REGISTRATION_ID)
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequests))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                        .defaultSuccessUrl("/backoffice", true)
                        .failureUrl("/login?error"))
                .oidcLogout(logout -> logout.backChannel(backChannel -> backChannel.logoutHandler(backChannelLogout)))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccess));
        return http.build();
    }

    @Bean
    public OidcSessionRegistry oidcSessionRegistry() {
        return new InMemoryOidcSessionRegistry();
    }

    @Bean
    public JwtDecoder jwtDecoder(OidcProperties oidc, LegacyProperties legacy) {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(oidc.issuerUri()).build();
            decoder.setJwtValidator(TokenValidators.accessToken(oidc.issuerUri(), legacy.tenantId()));
            return decoder;
        });
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OidcProperties oidc) {
        SingletonSupplier<ClientRegistration> registration = SingletonSupplier.of(() ->
                ClientRegistrations.fromIssuerLocation(oidc.issuerUri())
                        .registrationId(REGISTRATION_ID)
                        .clientId(oidc.clientId())
                        .clientSecret(oidc.clientSecret())
                        .scope("openid", "profile", "email")
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .build());
        return registrationId -> REGISTRATION_ID.equals(registrationId) ? registration.get() : null;
    }
}
