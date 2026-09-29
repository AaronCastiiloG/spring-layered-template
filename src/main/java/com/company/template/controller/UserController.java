package com.company.template.controller;

import com.company.template.dto.request.UserCreateRequest;
import com.company.template.dto.request.UserUpdateRequest;
import com.company.template.dto.response.UserPageResponse;
import com.company.template.dto.response.UserResponse;
import com.company.template.service.interfaces.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

	private final IUserService userService;

	@GetMapping
	public ResponseEntity<UserPageResponse> getUsers(@PageableDefault(size = 10) Pageable pageable) {
		return ResponseEntity.ok(userService.getAll(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
		return ResponseEntity.ok(userService.getById(id));
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
		return ResponseEntity.ok(userService.update(id, request));
	}

	@PostMapping("/{id}/deactivation")
	public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID id) {
		return ResponseEntity.ok(userService.deactivate(id));
	}

	@PostMapping("/{id}/activation")
	public ResponseEntity<UserResponse> activateUser(@PathVariable UUID id) {
		return ResponseEntity.ok(userService.activate(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
