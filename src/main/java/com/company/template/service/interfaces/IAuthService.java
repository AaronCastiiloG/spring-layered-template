package com.company.template.service.interfaces;

import com.company.template.dto.request.LoginRequest;
import com.company.template.dto.response.TokenResponse;

public interface IAuthService {

	TokenResponse login(LoginRequest request);

	TokenResponse refresh(String refreshToken);

	void logout(String refreshToken);

}
