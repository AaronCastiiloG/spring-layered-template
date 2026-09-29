package com.company.template.shared.security;

import com.company.template.entity.Account;
import com.company.template.entity.Role;
import com.company.template.shared.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

	private final JwtProperties properties = new JwtProperties("test-jwt-secret-key-at-least-32-bytes", 15, 7);
	private final JwtConfig jwtConfig = new JwtConfig();
	private final JwtUtil jwtService = new JwtUtil(jwtConfig.jwtEncoder(properties), properties);

	@Test
	void accessTokenCarriesSubjectAndType() {
		Account account = new Account();
		account.setUsername("ana");
		account.setRoles(Set.of(Role.USER));

		String token = jwtService.generateAccessToken(account);
		Jwt jwt = jwtConfig.jwtDecoder(properties).decode(token);

		assertThat(jwt.getSubject()).isEqualTo("ana");
		assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
	}

	@Test
	void refreshTokenIsDistinctFromAccessToken() {
		Account account = new Account();
		account.setUsername("ana");
		account.setRoles(Set.of(Role.ADMIN));

		String access = jwtService.generateAccessToken(account);
		String refresh = jwtService.generateRefreshToken(account);
		Jwt jwt = jwtConfig.jwtDecoder(properties).decode(refresh);

		assertThat(refresh).isNotEqualTo(access);
		assertThat(jwt.getClaimAsString("type")).isEqualTo("refresh");
	}

}
