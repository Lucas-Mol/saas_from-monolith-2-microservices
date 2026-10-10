package dev.agenda.legacy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String STAFF = "STAFF";
    private static final String CUSTOMER = "CUSTOMER";

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
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
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new RejectFormContentTypeFilter(), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain backofficeSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/backoffice/invoices", "/backoffice/invoices/**").hasRole(ADMIN)
                        .requestMatchers("/", "/backoffice", "/backoffice/**").hasAnyRole(ADMIN, STAFF)
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/backoffice", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
