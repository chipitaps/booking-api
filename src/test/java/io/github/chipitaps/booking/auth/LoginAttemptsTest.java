package io.github.chipitaps.booking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chipitaps.booking.config.JwtProperties;
import io.github.chipitaps.booking.support.TestClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginAttemptsTest {

	private final TestClock clock = new TestClock(Instant.parse("2030-01-07T12:00:00Z"));

	private final LoginAttempts attempts = new LoginAttempts(clock);

	private void fail(String email, int times) {
		for (int i = 0; i < times; i++) {
			attempts.recordFailure(email);
		}
	}

	@Test
	void blocksAnEmailAfterTooManyFailures() {
		fail("a@example.com", LoginAttempts.MAX_FAILURES - 1);
		assertThat(attempts.isBlocked("a@example.com")).isFalse();

		attempts.recordFailure("a@example.com");
		assertThat(attempts.isBlocked("a@example.com")).isTrue();
		assertThat(attempts.isBlocked("b@example.com")).isFalse();
	}

	@Test
	void unblocksWhenTheWindowEnds() {
		fail("a@example.com", LoginAttempts.MAX_FAILURES);

		clock.advance(LoginAttempts.WINDOW.minusSeconds(1));
		assertThat(attempts.isBlocked("a@example.com")).isTrue();

		clock.advance(Duration.ofSeconds(1));
		assertThat(attempts.isBlocked("a@example.com")).isFalse();
	}

	@Test
	void startsCountingAgainAfterTheWindow() {
		fail("a@example.com", LoginAttempts.MAX_FAILURES - 1);
		clock.advance(LoginAttempts.WINDOW);

		attempts.recordFailure("a@example.com");
		assertThat(attempts.isBlocked("a@example.com")).isFalse();
	}

	@Test
	void forgetsFailuresAfterASuccessfulLogin() {
		fail("a@example.com", LoginAttempts.MAX_FAILURES - 1);
		attempts.reset("a@example.com");
		attempts.recordFailure("a@example.com");

		assertThat(attempts.isBlocked("a@example.com")).isFalse();
	}

	@Test
	void refusesToStartWithAShortSigningSecret() {
		assertThatThrownBy(() -> new JwtProperties("too-short", "booking-api", Duration.ofHours(2)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("JWT_SECRET");
		assertThatThrownBy(() -> new JwtProperties(null, "booking-api", Duration.ofHours(2)))
			.isInstanceOf(IllegalStateException.class);
	}

}
