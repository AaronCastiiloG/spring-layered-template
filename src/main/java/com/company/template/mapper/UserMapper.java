package com.company.template.mapper;

import com.company.template.dto.request.UserCreateRequest;
import com.company.template.dto.request.UserUpdateRequest;
import com.company.template.dto.response.UserResponse;
import com.company.template.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
		componentModel = "spring",
		uses = UserLogMapper.class,
		unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "active", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "logs", ignore = true)
	User toEntity(UserCreateRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "active", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "logs", ignore = true)
	void update(UserUpdateRequest request, @MappingTarget User user);

	UserResponse toResponse(User user);

}
