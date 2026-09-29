package com.company.template.service.impl;

import com.company.template.dto.request.UserCreateRequest;
import com.company.template.dto.request.UserUpdateRequest;
import com.company.template.dto.response.UserPageResponse;
import com.company.template.dto.response.UserResponse;
import com.company.template.mapper.UserMapper;
import com.company.template.exception.BusinessRuleException;
import com.company.template.exception.ConflictException;
import com.company.template.exception.NotFoundException;
import com.company.template.entity.User;
import com.company.template.entity.UserLog;
import com.company.template.repository.UserRepository;
import com.company.template.service.interfaces.IUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService implements IUserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@Override
	@Transactional
	public UserResponse create(UserCreateRequest request) {
		if (userRepository.existsByName(request.getName())) {
			throw new ConflictException("USER_ALREADY_EXISTS", "A user with this name already exists");
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ConflictException("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
		}

		User user = userMapper.toEntity(request);
		addLog(user, "User created");
		return userMapper.toResponse(userRepository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserPageResponse getAll(Pageable pageable) {
		Page<User> page = userRepository.findAll(pageable);
		return new UserPageResponse(
				page.getContent().stream().map(userMapper::toResponse).toList(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.getNumber(),
				page.getSize()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getById(UUID id) {
		return userMapper.toResponse(findUser(id));
	}

	@Override
	@Transactional
	public UserResponse update(UUID id, UserUpdateRequest request) {
		User user = findUser(id);
		if (!user.isActive()) {
			throw new BusinessRuleException("USER_INACTIVE", "Inactive users cannot be updated");
		}
		if (userRepository.existsByNameAndIdNot(request.getName(), id)) {
			throw new ConflictException("USER_ALREADY_EXISTS", "A user with this name already exists");
		}
		if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
			throw new ConflictException("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
		}
		userMapper.update(request, user);
		addLog(user, "User updated");
		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserResponse deactivate(UUID id) {
		User user = findUser(id);
		if (!user.isActive()) {
			throw new BusinessRuleException("USER_ALREADY_INACTIVE", "The user is already inactive");
		}
		user.setActive(false);
		addLog(user, "User deactivated");
		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserResponse activate(UUID id) {
		User user = findUser(id);
		if (user.isActive()) {
			throw new BusinessRuleException("USER_ALREADY_ACTIVE", "The user is already active");
		}
		user.setActive(true);
		addLog(user, "User activated");
		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		User user = findUser(id);
		userRepository.delete(user);
	}

	private User findUser(UUID id) {
		return userRepository.findByIdWithLogs(id)
				.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User was not found"));
	}

	private void addLog(User user, String message) {
		UserLog log = new UserLog();
		log.setLogMessage(message);
		log.setUser(user);
		user.getLogs().add(log);
	}

}
