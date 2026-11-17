package io.github.chipitaps.booking.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chipitaps.booking.TestcontainersConfiguration;
import io.github.chipitaps.booking.user.AppUser;
import io.github.chipitaps.booking.user.Role;
import io.github.chipitaps.booking.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

	protected static final String PASSWORD = "correct horse battery";

	@LocalServerPort
	private int port;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected TestClock clock;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	protected Api api;

	@BeforeEach
	void resetState() {
		jdbc.execute("truncate bookings, spaces, users cascade");
		clock.set(TestcontainersConfiguration.NOW);
		api = new Api(port);
	}

	protected static String uniqueEmail(String name) {
		return name.toLowerCase().replace(' ', '.') + "+" + UUID.randomUUID().toString().substring(0, 8)
				+ "@example.com";
	}

	protected String register(String name) {
		return registerWithEmail(name, uniqueEmail(name));
	}

	protected String registerWithEmail(String name, String email) {
		Api.Response response = api.post("/api/auth/register", null, """
				{"email": "%s", "name": "%s", "password": "%s"}""".formatted(email, name, PASSWORD));
		assertThat(response.status()).as(response.body()).isEqualTo(201);
		return response.json("$.accessToken");
	}

	protected String admin() {
		String email = uniqueEmail("admin");
		users.save(new AppUser(email, "Admin", passwordEncoder.encode(PASSWORD), Role.ADMIN, clock.instant()));
		Api.Response response = api.post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}""".formatted(email, PASSWORD));
		assertThat(response.status()).as(response.body()).isEqualTo(200);
		return response.json("$.accessToken");
	}

	protected String createSpace(String adminToken, String name, String timeZone, String opensAt, String closesAt) {
		Api.Response response = api.post("/api/spaces", adminToken, spaceJson(name, timeZone, opensAt, closesAt));
		assertThat(response.status()).as(response.body()).isEqualTo(201);
		return response.json("$.id");
	}

	protected String createSpace(String name) {
		return createSpace(admin(), name, "UTC", "08:00", "20:00");
	}

	protected static String spaceJson(String name, String timeZone, String opensAt, String closesAt) {
		return """
				{"name": "%s", "description": "Test space", "capacity": 6, "timeZone": "%s",
				 "opensAt": "%s", "closesAt": "%s"}""".formatted(name, timeZone, opensAt, closesAt);
	}

	protected static String bookingJson(String spaceId, String startsAt, String endsAt) {
		return """
				{"spaceId": "%s", "title": "Team sync", "startsAt": "%s", "endsAt": "%s"}"""
			.formatted(spaceId, startsAt, endsAt);
	}

	protected Api.Response book(String token, String spaceId, String startsAt, String endsAt) {
		return api.post("/api/bookings", token, bookingJson(spaceId, startsAt, endsAt));
	}

}
