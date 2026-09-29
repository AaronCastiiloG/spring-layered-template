package com.company.template.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

	public BusinessRuleException(String code, String message) {
		super(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
	}

}
