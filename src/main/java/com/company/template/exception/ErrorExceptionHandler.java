package com.company.template.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle(exception.getStatus().getReasonPhrase());
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleModelValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.toList();
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"The request parameters are invalid"
		);
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"The request conflicts with existing data"
		);
		problem.setTitle("Conflict");
		problem.setProperty("code", conflictCode(exception));
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneral(Exception exception) {
		log.error("Unhandled exception", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Internal server error, contact support team"
		);
		problem.setTitle("Internal Server Error");
		problem.setProperty("code", "INTERNAL_SERVER_ERROR");
		return problem;
	}

	private String conflictCode(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof ConstraintViolationException constraint && constraint.getConstraintName() != null) {
				String code = codeForConstraint(constraint.getConstraintName());
				if (code != null) {
					return code;
				}
			}
			String message = current.getMessage();
			if (message != null) {
				String code = codeForConstraint(message);
				if (code != null) {
					return code;
				}
			}
			current = current.getCause();
		}
		return "DATA_CONFLICT";
	}

	private String codeForConstraint(String value) {
		if (value.contains("uk_users_name")) {
			return "USER_ALREADY_EXISTS";
		}
		if (value.contains("uk_users_email")) {
			return "EMAIL_ALREADY_EXISTS";
		}
		if (value.contains("uk_accounts_username")) {
			return "USERNAME_ALREADY_EXISTS";
		}
		return null;
	}

}
