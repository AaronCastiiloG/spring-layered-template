package com.company.template.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

	private UUID id;
	private String name;
	private String address;
	private boolean active;
	private int age;
	private String email;
	private String phoneNumber;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<UserLogResponse> logs;

}
