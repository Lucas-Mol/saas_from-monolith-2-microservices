package dev.agenda.legacy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "legacy")
public record LegacyProperties(String instanceName, int cancellationMinHours, String tenantId) {
}
