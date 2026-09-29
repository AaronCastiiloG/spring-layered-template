package com.company.template.shared.security;

import com.company.template.entity.Account;
import com.company.template.shared.config.JwtProperties;
import com.company.template.shared.security.JwtConfig;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class JwtUtil {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public JwtUtil(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public String generateAccessToken(Account account) {
		Instant now = Instant.now();
		return encode(account, "access", now, now.plus(jwtProperties.accessTokenMinutes(), ChronoUnit.MINUTES));
	}

	public String generateRefreshToken(Account account) {
		Instant now = Instant.now();
		return encode(account, "refresh", now, now.plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS));
	}

	private String encode(Account account, String type, Instant issuedAt, Instant expiresAt) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).keyId(JwtConfig.KEY_ID).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.id(UUID.randomUUID().toString())
				.subject(account.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("type", type)
				.claim("roles", account.getRoles().stream().map(Enum::name).toList())
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
