package com.company.template.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
		boolean enabled,
		String adminUsername,
		String adminPassword,
		String userUsername,
		String userPassword
) {
}
