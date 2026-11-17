package io.github.chipitaps.booking.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chipitaps.booking.support.Api;
import io.github.chipitaps.booking.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuthIntegrationTest extends IntegrationTest {

	private Api.Response login(String email, String password) {
		return api.post("/api/auth/login", null, """
				{"email": "%s", "password": "%s"}""".formatted(email, password));
	}

	@Test
	void registeringReturnsATokenThatIdentifiesTheUser() {
		String token = register("Ana Torres");

		Api.Response me = api.get("/api/me", token);

		assertThat(me.status()).isEqualTo(200);
		assertThat(me.<String>json("$.name")).isEqualTo("Ana Torres");
		assertThat(me.<String>json("$.role")).isEqualTo("USER");
	}

	@Test
	void emailsAreUniqueRegardlessOfCase() {
		String email = uniqueEmail("ana");
		registerWithEmail("Ana", email);

		Api.Response again = api.post("/api/auth/register", null, """
				{"email": "  %s ", "name": "Ana", "password": "%s"}""".formatted(email.toUpperCase(), PASSWORD));

		assertThat(again.status()).isEqualTo(409);
		assertThat(again.<String>json("$.detail")).contains("already exists");
	}

	@Test
	void invalidRegistrationsListEveryBadField() {
		Api.Response response = api.post("/api/auth/register", null, """
				{"email": "not-an-email", "name": "", "password": "short"}""");

		assertThat(response.status()).isEqualTo(400);
		assertThat(response.<String>json("$.errors.email")).isEqualTo("Enter a valid email address.");
		assertThat(response.<String>json("$.errors.name")).isEqualTo("Enter your name.");
		assertThat(response.<String>json("$.errors.password")).isEqualTo("Use between 10 and 72 characters.");
	}

	@Test
	void loginChecksThePasswordAndNormalisesTheEmail() {
		String email = uniqueEmail("ben");
		registerWithEmail("Ben", email);

		assertThat(login(email, "wrong password").status()).isEqualTo(401);
		assertThat(login("missing-" + email, PASSWORD).status()).isEqualTo(401);
		Api.Response ok = login(email.toUpperCase(), PASSWORD);
		assertThat(ok.status()).isEqualTo(200);
		assertThat(ok.<String>json("$.tokenType")).isEqualTo("Bearer");
	}

	@Test
	void repeatedFailuresLockTheAccountForAWhile() {
		String email = uniqueEmail("cleo");
		registerWithEmail("Cleo", email);

		for (int i = 0; i < 5; i++) {
			assertThat(login(email, "wrong password").status()).isEqualTo(401);
		}
		assertThat(login(email, PASSWORD).status()).isEqualTo(429);

		clock.advance(Duration.ofMinutes(15));
		assertThat(login(email, PASSWORD).status()).isEqualTo(200);
	}

	@Test
	void requestsWithoutAValidTokenAreRejected() {
		assertThat(api.get("/api/me", null).status()).isEqualTo(401);
		assertThat(api.get("/api/me", "not.a.token").status()).isEqualTo(401);

		String token = register("Dan");
		assertThat(api.get("/api/me", token).status()).isEqualTo(200);
		assertThat(api.get("/api/me", promoteToAdmin(token)).status()).isEqualTo(401);
	}

	@Test
	void editingTheRoleInsideATokenDoesNotGrantAdminRights() {
		String forged = promoteToAdmin(register("Mallory"));

		Api.Response response = api.post("/api/spaces", forged, spaceJson("Vault", "UTC", "08:00", "20:00"));

		assertThat(response.status()).isEqualTo(401);
	}

	private static String promoteToAdmin(String token) {
		String[] parts = token.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		assertThat(payload).contains("\"roles\":[\"USER\"]");
		String forged = payload.replace("\"roles\":[\"USER\"]", "\"roles\":[\"ADMIN\"]");
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8));
		return parts[0] + "." + encoded + "." + parts[2];
	}

	@Test
	void tokensExpire() {
		String token = register("Eve");

		clock.advance(Duration.ofHours(2).minusMinutes(5));
		assertThat(api.get("/api/me", token).status()).isEqualTo(200);

		clock.advance(Duration.ofMinutes(10));
		assertThat(api.get("/api/me", token).status()).isEqualTo(401);
	}

}
