package com.company.template.mapper;

import com.company.template.dto.response.UserLogResponse;
import com.company.template.entity.UserLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserLogMapper {

	UserLogResponse toResponse(UserLog log);

}
