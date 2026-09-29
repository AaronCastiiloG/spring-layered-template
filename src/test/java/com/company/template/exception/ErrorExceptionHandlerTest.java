package com.company.template.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorExceptionHandlerTest {

	private final ErrorExceptionHandler handler = new ErrorExceptionHandler();

	@Test
	void mapsKnownConstraintToConflictCode() {
		DataIntegrityViolationException exception = new DataIntegrityViolationException(
				"could not execute statement [uk_users_name]"
		);

		ProblemDetail problem = handler.handleDataIntegrity(exception);

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getProperties()).containsEntry("code", "USER_ALREADY_EXISTS");
	}

	@Test
	void hidesUnexpectedErrors() {
		ProblemDetail problem = handler.handleGeneral(new IllegalStateException("database password"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problem.getDetail()).doesNotContain("database password");
		assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_SERVER_ERROR");
	}

}
