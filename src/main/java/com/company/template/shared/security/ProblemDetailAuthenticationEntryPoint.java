package com.company.template.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper jsonMapper;

	public ProblemDetailAuthenticationEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException
	) throws IOException {
		Object authError = request.getAttribute(JwtAuthFilter.AUTH_ERROR);
		if ("INVALID_TOKEN".equals(authError)) {
			write(response, 401, "Unauthorized", "INVALID_TOKEN", "The access token is invalid or expired");
			return;
		}
		write(response, 401, "Unauthorized", "UNAUTHORIZED", "Authentication is required");
	}

	private void write(
			HttpServletResponse response,
			int status,
			String title,
			String code,
			String detail
	) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		body.put("status", status);
		body.put("detail", detail);
		body.put("code", code);
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
