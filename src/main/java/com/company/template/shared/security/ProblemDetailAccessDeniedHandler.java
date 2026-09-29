package com.company.template.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	public ProblemDetailAccessDeniedHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException
	) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", "Forbidden");
		body.put("status", 403);
		body.put("detail", "You do not have permission to perform this action");
		body.put("code", "FORBIDDEN");
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
