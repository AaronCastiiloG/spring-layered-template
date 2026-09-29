package com.company.template.service.impl;

import com.company.template.dto.request.LoginRequest;
import com.company.template.dto.response.TokenResponse;
import com.company.template.shared.security.JwtUtil;
import com.company.template.exception.UnauthorizedException;
import com.company.template.entity.Account;
import com.company.template.entity.RefreshToken;
import com.company.template.repository.AccountRepository;
import com.company.template.repository.RefreshTokenRepository;
import com.company.template.service.interfaces.IAuthService;
import com.company.template.shared.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService implements IAuthService {

	static final String INVALID_CREDENTIALS = "Username or password is incorrect";
	static final String INVALID_REFRESH = "The refresh token is invalid or expired";

	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtService;
	private final JwtDecoder jwtDecoder;
	private final TokenHasher tokenHasher;
	private final JwtProperties jwtProperties;

	public AuthService(
			AccountRepository accountRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtUtil jwtService,
			JwtDecoder jwtDecoder,
			TokenHasher tokenHasher,
			JwtProperties jwtProperties
	) {
		this.accountRepository = accountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.jwtDecoder = jwtDecoder;
		this.tokenHasher = tokenHasher;
		this.jwtProperties = jwtProperties;
	}

	@Override
	@Transactional
	public TokenResponse login(LoginRequest request) {
		Account account = accountRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS);
		}
		return issueTokens(account);
	}

	@Override
	@Transactional
	public TokenResponse refresh(String refreshToken) {
		Jwt jwt = decodeRefreshToken(refreshToken);
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHasher.sha256(refreshToken))
				.orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", INVALID_REFRESH));
		if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new UnauthorizedException("INVALID_REFRESH_TOKEN", INVALID_REFRESH);
		}
		if (!stored.getAccount().getUsername().equals(jwt.getSubject())) {
			throw new UnauthorizedException("INVALID_REFRESH_TOKEN", INVALID_REFRESH);
		}
		stored.setRevoked(true);
		return issueTokens(stored.getAccount());
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		try {
			Jwt jwt = jwtDecoder.decode(refreshToken);
			if (!"refresh".equals(jwt.getClaim("type"))) {
				return;
			}
		}
		catch (JwtException exception) {
			return;
		}
		refreshTokenRepository.findByTokenHash(tokenHasher.sha256(refreshToken))
				.ifPresent(stored -> stored.setRevoked(true));
	}

	private Jwt decodeRefreshToken(String refreshToken) {
		try {
			Jwt jwt = jwtDecoder.decode(refreshToken);
			if (!"refresh".equals(jwt.getClaim("type"))) {
				throw new UnauthorizedException("INVALID_REFRESH_TOKEN", INVALID_REFRESH);
			}
			return jwt;
		}
		catch (JwtException exception) {
			throw new UnauthorizedException("INVALID_REFRESH_TOKEN", INVALID_REFRESH);
		}
	}

	private TokenResponse issueTokens(Account account) {
		String accessToken = jwtService.generateAccessToken(account);
		String refreshToken = jwtService.generateRefreshToken(account);

		RefreshToken stored = new RefreshToken();
		stored.setAccount(account);
		stored.setTokenHash(tokenHasher.sha256(refreshToken));
		stored.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshTokenDays()));
		stored.setRevoked(false);
		refreshTokenRepository.save(stored);

		return new TokenResponse(accessToken, refreshToken, jwtProperties.accessTokenMinutes() * 60);
	}

}
