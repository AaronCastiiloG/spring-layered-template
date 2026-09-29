package com.company.template;

import com.company.template.entity.Account;
import com.company.template.entity.Role;
import com.company.template.repository.AccountRepository;
import com.company.template.repository.RefreshTokenRepository;
import com.company.template.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ApiIT {

	private static final String CONTEXT = "/api/v1";
	private static final String PASSWORD = "password123";
	private static final String USER_JSON = """
			{
			  "name": "Maria Lopez",
			  "address": "Calle 10",
			  "age": 28,
			  "email": "maria@example.com",
			  "phoneNumber": "3001234567"
			}
			""";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void seedAccounts() {
		refreshTokenRepository.deleteAll();
		accountRepository.deleteAll();
		userRepository.deleteAll();
		saveAccount("admin", Role.ADMIN);
		saveAccount("user", Role.USER);
	}

	@Test
	void loginReturnsTokens() throws Exception {
		mockMvc.perform(json(post(CONTEXT + "/auth/login"), credentials("user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.expiresInSeconds").value(900));
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		mockMvc.perform(json(post(CONTEXT + "/auth/login"), credentials("user").replace(PASSWORD, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void protectedRouteRequiresToken() throws Exception {
		mockMvc.perform(json(get(CONTEXT + "/users"), null))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void authenticatedUserCanListUsers() throws Exception {
		mockMvc.perform(json(get(CONTEXT + "/users"), null).header(HttpHeaders.AUTHORIZATION, login("user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").isNumber())
				.andExpect(jsonPath("$.totalPages").isNumber())
				.andExpect(jsonPath("$.number").value(0))
				.andExpect(jsonPath("$.size").value(10));
	}

	@Test
	void userRoleCannotDelete() throws Exception {
		String userId = createUser(login("admin"));

		mockMvc.perform(json(delete(CONTEXT + "/users/" + userId), null).header(HttpHeaders.AUTHORIZATION, login("user")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void adminCanDelete() throws Exception {
		String admin = login("admin");
		String userId = createUser(admin);

		mockMvc.perform(json(delete(CONTEXT + "/users/" + userId), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isNoContent());

		mockMvc.perform(json(get(CONTEXT + "/users/" + userId), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		MvcResult login = mockMvc.perform(json(post(CONTEXT + "/auth/login"), credentials("user")))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

		mockMvc.perform(json(post(CONTEXT + "/auth/logout"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(json(post(CONTEXT + "/auth/refresh"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void refreshRotatesTokenAndRejectsReuse() throws Exception {
		MvcResult login = mockMvc.perform(json(post(CONTEXT + "/auth/login"), credentials("user")))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

		mockMvc.perform(json(post(CONTEXT + "/auth/refresh"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		mockMvc.perform(json(post(CONTEXT + "/auth/refresh"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void createReturnsMappedUserAndRejectsDuplicatesAndInvalidBodies() throws Exception {
		String admin = login("admin");

		MvcResult created = mockMvc.perform(json(post(CONTEXT + "/users"), USER_JSON).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Maria Lopez"))
				.andExpect(jsonPath("$.address").value("Calle 10"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.age").value(28))
				.andExpect(jsonPath("$.email").value("maria@example.com"))
				.andExpect(jsonPath("$.phoneNumber").value("3001234567"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.logs[0].logMessage").value("User created"))
				.andExpect(jsonPath("$.logs[0].createdAt").isNotEmpty())
				.andReturn();

		mockMvc.perform(json(post(CONTEXT + "/users"), USER_JSON).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));

		mockMvc.perform(json(post(CONTEXT + "/users"), "{}").header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors").isArray());

		String userId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(json(get(CONTEXT + "/users/" + userId), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Maria Lopez"));
	}

	@Test
	void inactiveUserCannotBeUpdatedUntilActivated() throws Exception {
		String admin = login("admin");
		String user = login("user");
		String userId = createUser(admin);

		mockMvc.perform(json(post(CONTEXT + "/users/" + userId + "/deactivation"), null).header(HttpHeaders.AUTHORIZATION, user))
				.andExpect(status().isForbidden());

		mockMvc.perform(json(post(CONTEXT + "/users/" + userId + "/deactivation"), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(json(post(CONTEXT + "/users/" + userId + "/deactivation"), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("USER_ALREADY_INACTIVE"));

		mockMvc.perform(json(put(CONTEXT + "/users/" + userId), USER_JSON).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("USER_INACTIVE"));

		mockMvc.perform(json(post(CONTEXT + "/users/" + userId + "/activation"), null).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void unknownUserIdIsNotFound() throws Exception {
		mockMvc.perform(json(get(CONTEXT + "/users/" + UUID.randomUUID()), null).header(HttpHeaders.AUTHORIZATION, login("user")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	private String createUser(String bearer) throws Exception {
		MvcResult result = mockMvc.perform(json(post(CONTEXT + "/users"), USER_JSON).header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String login(String username) throws Exception {
		MvcResult result = mockMvc.perform(json(post(CONTEXT + "/auth/login"), credentials(username)))
				.andExpect(status().isOk())
				.andReturn();
		return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private String credentials(String username) {
		return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
		builder.contextPath(CONTEXT).contentType(MediaType.APPLICATION_JSON);
		if (body != null) {
			builder.content(body);
		}
		return builder;
	}

	private void saveAccount(String username, Role role) {
		Account account = new Account();
		account.setUsername(username);
		account.setPasswordHash(passwordEncoder.encode(PASSWORD));
		account.setRoles(new HashSet<>(Set.of(role)));
		accountRepository.save(account);
	}

}
