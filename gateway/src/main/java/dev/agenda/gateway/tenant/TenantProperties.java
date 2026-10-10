package dev.agenda.gateway.tenant;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("tenancy")
public record TenantProperties(@NotBlank String baseDomain) {
}
