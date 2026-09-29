package com.company.template.dto.request;

import com.company.template.shared.constants.RegexConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

	@NotBlank(message = "Name is required")
	@Size(min = 5, max = 50, message = "Name must be between 5 and 50 characters")
	private String name;

	@Size(min = 5, max = 100, message = "Address must be between 5 and 100 characters")
	private String address;

	@Positive(message = "Age must be greater than zero")
	private int age;

	@NotBlank(message = "Email is required")
	@Email(message = "The email format is not valid")
	private String email;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = RegexConstants.PHONE, message = "Phone number must contain 10 digits")
	private String phoneNumber;

}
