package com.company.template.service.interfaces;

import com.company.template.dto.request.UserCreateRequest;
import com.company.template.dto.request.UserUpdateRequest;
import com.company.template.dto.response.UserPageResponse;
import com.company.template.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IUserService {

	UserResponse create(UserCreateRequest request);

	UserPageResponse getAll(Pageable pageable);

	UserResponse getById(UUID id);

	UserResponse update(UUID id, UserUpdateRequest request);

	UserResponse deactivate(UUID id);

	UserResponse activate(UUID id);

	void delete(UUID id);

}
