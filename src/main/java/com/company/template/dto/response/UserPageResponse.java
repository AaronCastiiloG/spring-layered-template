package com.company.template.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserPageResponse {

	private final List<UserResponse> content;
	private final long totalElements;
	private final int totalPages;
	private final int number;
	private final int size;

}
