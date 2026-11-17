package io.github.chipitaps.booking.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginAttempts {

	static final int MAX_FAILURES = 5;

	static final Duration WINDOW = Duration.ofMinutes(15);

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final Clock clock;

	LoginAttempts(Clock clock) {
		this.clock = clock;
	}

	public boolean isBlocked(String email) {
		Window window = current(email);
		return window != null && window.failures() >= MAX_FAILURES;
	}

	public void recordFailure(String email) {
		Instant now = clock.instant();
		windows.compute(email, (key, window) -> (window == null || expired(window, now)) ? new Window(1, now)
				: new Window(window.failures() + 1, window.startedAt()));
	}

	public void reset(String email) {
		windows.remove(email);
	}

	private Window current(String email) {
		Window window = windows.get(email);
		if (window != null && expired(window, clock.instant())) {
			windows.remove(email, window);
			return null;
		}
		return window;
	}

	private static boolean expired(Window window, Instant now) {
		return !now.isBefore(window.startedAt().plus(WINDOW));
	}

	private record Window(int failures, Instant startedAt) {
	}

}
