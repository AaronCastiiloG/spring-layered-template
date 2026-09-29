package com.company.template.shared.security;

import com.company.template.entity.Account;
import com.company.template.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

	public static final String AUTH_ERROR = "authErrorCode";

	private final JwtDecoder jwtDecoder;
	private final AccountRepository accountRepository;

	public JwtAuthFilter(JwtDecoder jwtDecoder, AccountRepository accountRepository) {
		this.jwtDecoder = jwtDecoder;
		this.accountRepository = accountRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Jwt jwt = jwtDecoder.decode(header.substring(7));
			if (!"access".equals(jwt.getClaim("type"))) {
				request.setAttribute(AUTH_ERROR, "INVALID_TOKEN");
			}
			else {
				authenticate(request, jwt);
			}
		}
		catch (JwtException exception) {
			SecurityContextHolder.clearContext();
			request.setAttribute(AUTH_ERROR, "INVALID_TOKEN");
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, Jwt jwt) {
		Account account = accountRepository.findByUsername(jwt.getSubject()).orElse(null);
		if (account == null) {
			request.setAttribute(AUTH_ERROR, "INVALID_TOKEN");
			return;
		}

		List<SimpleGrantedAuthority> authorities = account.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
				.toList();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				account.getUsername(),
				null,
				authorities
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
