package com.company.template.shared.config;

import com.company.template.shared.constants.AppConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title(AppConstants.API_TITLE)
						.version(AppConstants.API_VERSION)
						.description("Layered API template"))
				.addSecurityItem(new SecurityRequirement().addList(AppConstants.BEARER_SCHEME))
				.components(new Components().addSecuritySchemes(
						AppConstants.BEARER_SCHEME,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
				));
	}

}
