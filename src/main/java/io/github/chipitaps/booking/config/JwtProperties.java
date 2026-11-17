package io.github.chipitaps.booking.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl) {

	public static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"Set JWT_SECRET to a random value of at least " + MIN_SECRET_BYTES + " characters.");
		}
	}

}
