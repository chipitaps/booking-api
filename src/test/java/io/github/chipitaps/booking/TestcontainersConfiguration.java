package io.github.chipitaps.booking;

import io.github.chipitaps.booking.support.TestClock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	@Primary
	TestClock testClock() {
		return new TestClock(NOW);
	}

}
