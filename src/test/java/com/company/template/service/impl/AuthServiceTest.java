package com.company.template.service.impl;

import com.company.template.dto.request.LoginRequest;
import com.company.template.dto.response.TokenResponse;
import com.company.template.shared.security.JwtUtil;
import com.company.template.exception.UnauthorizedException;
import com.company.template.entity.Account;
import com.company.template.entity.RefreshToken;
import com.company.template.entity.Role;
import com.company.template.repository.AccountRepository;
import com.company.template.repository.RefreshTokenRepository;
import com.company.template.shared.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtService;

	@Mock
	private JwtDecoder jwtDecoder;

	private final TokenHasher tokenHasher = new TokenHasher();
	private final JwtProperties jwtProperties = new JwtProperties("test-jwt-secret-key-at-least-32-bytes", 15, 7);
	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				accountRepository,
				refreshTokenRepository,
				passwordEncoder,
				jwtService,
				jwtDecoder,
				tokenHasher,
				jwtProperties
		);
	}

	@Test
	void loginRejectsUnknownUsername() {
		LoginRequest request = loginRequest("ana", "password123");
		when(accountRepository.findByUsername("ana")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage(AuthService.INVALID_CREDENTIALS)
				.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	@Test
	void loginRejectsWrongPasswordWithTheSameError() {
		LoginRequest request = loginRequest("ana", "password123");
		Account account = account("ana");
		when(accountRepository.findByUsername("ana")).thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage(AuthService.INVALID_CREDENTIALS)
				.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	@Test
	void loginIssuesTokens() {
		LoginRequest request = loginRequest("ana", "password123");
		Account account = account("ana");
		when(accountRepository.findByUsername("ana")).thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(true);
		when(jwtService.generateAccessToken(account)).thenReturn("access");
		when(jwtService.generateRefreshToken(account)).thenReturn("refresh");

		TokenResponse response = authService.login(request);

		assertThat(response.getAccessToken()).isEqualTo("access");
		assertThat(response.getRefreshToken()).isEqualTo("refresh");
		assertThat(response.getExpiresInSeconds()).isEqualTo(900);
		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(saved.capture());
		assertThat(saved.getValue().getTokenHash()).isEqualTo(tokenHasher.sha256("refresh"));
		assertThat(saved.getValue().isRevoked()).isFalse();
	}

	@Test
	void refreshRotatesTokenAndRevokesPrevious() {
		Account account = account("ana");
		String current = "current-refresh";
		RefreshToken stored = new RefreshToken();
		stored.setAccount(account);
		stored.setRevoked(false);
		stored.setExpiresAt(LocalDateTime.now().plusDays(1));
		stored.setTokenHash(tokenHasher.sha256(current));
		when(jwtDecoder.decode(current)).thenReturn(refreshJwt(current, "ana"));
		when(refreshTokenRepository.findByTokenHash(tokenHasher.sha256(current))).thenReturn(Optional.of(stored));
		when(jwtService.generateAccessToken(account)).thenReturn("new-access");
		when(jwtService.generateRefreshToken(account)).thenReturn("new-refresh");

		TokenResponse response = authService.refresh(current);

		assertThat(stored.isRevoked()).isTrue();
		assertThat(response.getAccessToken()).isEqualTo("new-access");
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void logoutRevokesStoredRefreshToken() {
		Account account = account("ana");
		String current = "current-refresh";
		RefreshToken stored = new RefreshToken();
		stored.setAccount(account);
		stored.setRevoked(false);
		stored.setExpiresAt(LocalDateTime.now().plusDays(1));
		when(jwtDecoder.decode(current)).thenReturn(refreshJwt(current, "ana"));
		when(refreshTokenRepository.findByTokenHash(tokenHasher.sha256(current))).thenReturn(Optional.of(stored));

		authService.logout(current);

		assertThat(stored.isRevoked()).isTrue();
	}

	@Test
	void refreshRejectsRevokedToken() {
		Account account = account("ana");
		String current = "current-refresh";
		RefreshToken stored = new RefreshToken();
		stored.setAccount(account);
		stored.setRevoked(true);
		stored.setExpiresAt(LocalDateTime.now().plusDays(1));
		when(jwtDecoder.decode(current)).thenReturn(refreshJwt(current, "ana"));
		when(refreshTokenRepository.findByTokenHash(tokenHasher.sha256(current))).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh(current))
				.isInstanceOf(UnauthorizedException.class)
				.hasFieldOrPropertyWithValue("code", "INVALID_REFRESH_TOKEN");
	}

	private LoginRequest loginRequest(String username, String password) {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		return request;
	}

	private Account account(String username) {
		Account account = new Account();
		account.setUsername(username);
		account.setPasswordHash("hash");
		account.setRoles(Set.of(Role.USER));
		return account;
	}

	private Jwt refreshJwt(String token, String username) {
		return Jwt.withTokenValue(token)
				.header("alg", "HS256")
				.subject(username)
				.claim("type", "refresh")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}

}
