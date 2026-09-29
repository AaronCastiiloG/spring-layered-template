package com.company.template.service.impl;

import com.company.template.dto.request.UserCreateRequest;
import com.company.template.dto.request.UserUpdateRequest;
import com.company.template.dto.response.UserResponse;
import com.company.template.mapper.UserMapper;
import com.company.template.exception.BusinessRuleException;
import com.company.template.exception.ConflictException;
import com.company.template.exception.NotFoundException;
import com.company.template.entity.User;
import com.company.template.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserMapper userMapper;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, userMapper);
	}

	@Test
	void createPersistsUserWithInitialLog() {
		UserCreateRequest request = request();
		User user = new User();
		UserResponse response = new UserResponse();
		when(userRepository.existsByName(request.getName())).thenReturn(false);
		when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
		when(userMapper.toEntity(request)).thenReturn(user);
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toResponse(user)).thenReturn(response);

		UserResponse created = userService.create(request);

		assertThat(created).isSameAs(response);
		assertThat(user.getLogs()).hasSize(1);
		assertThat(user.getLogs().get(0).getLogMessage()).isEqualTo("User created");
		assertThat(user.getLogs().get(0).getUser()).isSameAs(user);
	}

	@Test
	void createRejectsDuplicateName() {
		UserCreateRequest request = request();
		when(userRepository.existsByName(request.getName())).thenReturn(true);

		assertThatThrownBy(() -> userService.create(request))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("code", "USER_ALREADY_EXISTS");
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createRejectsDuplicateEmail() {
		UserCreateRequest request = request();
		when(userRepository.existsByName(request.getName())).thenReturn(false);
		when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

		assertThatThrownBy(() -> userService.create(request))
				.isInstanceOf(ConflictException.class)
				.hasFieldOrPropertyWithValue("code", "EMAIL_ALREADY_EXISTS");
	}

	@Test
	void getByIdRejectsMissingUser() {
		UUID id = UUID.randomUUID();
		when(userRepository.findByIdWithLogs(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getById(id))
				.isInstanceOf(NotFoundException.class)
				.hasFieldOrPropertyWithValue("code", "USER_NOT_FOUND");
	}

	@Test
	void updateRejectsInactiveUser() {
		UUID id = UUID.randomUUID();
		User user = new User();
		user.setActive(false);
		when(userRepository.findByIdWithLogs(id)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.update(id, updateRequest()))
				.isInstanceOf(BusinessRuleException.class)
				.hasFieldOrPropertyWithValue("code", "USER_INACTIVE");
	}

	@Test
	void deactivateRejectsUserAlreadyInactive() {
		UUID id = UUID.randomUUID();
		User user = new User();
		user.setActive(false);
		when(userRepository.findByIdWithLogs(id)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.deactivate(id))
				.isInstanceOf(BusinessRuleException.class)
				.hasFieldOrPropertyWithValue("code", "USER_ALREADY_INACTIVE");
	}

	private UserCreateRequest request() {
		UserCreateRequest request = new UserCreateRequest();
		request.setName("Maria Lopez");
		request.setAddress("Calle 10");
		request.setAge(28);
		request.setEmail("maria@example.com");
		request.setPhoneNumber("3001234567");
		return request;
	}

	private UserUpdateRequest updateRequest() {
		UserUpdateRequest request = new UserUpdateRequest();
		request.setName("Maria Lopez");
		request.setAddress("Calle 10");
		request.setAge(28);
		request.setEmail("maria@example.com");
		request.setPhoneNumber("3001234567");
		return request;
	}

}
