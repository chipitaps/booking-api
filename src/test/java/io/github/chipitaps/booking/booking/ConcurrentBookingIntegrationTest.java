package io.github.chipitaps.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.chipitaps.booking.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ConcurrentBookingIntegrationTest extends IntegrationTest {

	private static final int CUSTOMERS = 20;

	@Test
	void whenManyPeopleBookTheSameSlotAtOnceExactlyOneWins() throws Exception {
		String space = createSpace("Board Room");
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < CUSTOMERS; i++) {
			tokens.add(register("Customer " + i));
		}

		CountDownLatch ready = new CountDownLatch(CUSTOMERS);
		CountDownLatch go = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS)) {
			for (String token : tokens) {
				results.add(pool.submit(() -> {
					ready.countDown();
					go.await();
					return book(token, space, "2030-01-08T09:00:00Z", "2030-01-08T10:30:00Z").status();
				}));
			}
			ready.await();
			go.countDown();
		}

		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		assertThat(statuses).filteredOn((status) -> status == 201).hasSize(1);
		assertThat(statuses).filteredOn((status) -> status == 409).hasSize(CUSTOMERS - 1);
		assertThat(jdbc.queryForObject("select count(*) from bookings where status = 'CONFIRMED'", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void theDatabaseRejectsOverlapsEvenWhenTheApplicationIsBypassed() {
		UUID user = UUID.randomUUID();
		UUID space = UUID.randomUUID();
		Instant now = Instant.parse("2030-01-07T12:00:00Z");
		jdbc.update("insert into users (id, email, name, password_hash, role, created_at) values (?, ?, ?, ?, ?, ?)",
				user, "raw@example.com", "Raw", "x", "USER", Timestamp.from(now));
		jdbc.update("""
				insert into spaces (id, name, capacity, time_zone, opens_at, closes_at, created_at)
				values (?, 'Raw Room', 2, 'UTC', '08:00', '20:00', ?)""", space, Timestamp.from(now));

		insertBooking(space, user, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z", "CONFIRMED");
		insertBooking(space, user, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z", "CANCELLED");
		insertBooking(space, user, "2030-01-08T10:00:00Z", "2030-01-08T11:00:00Z", "CONFIRMED");

		Throwable overlap = catchThrowable(
				() -> insertBooking(space, user, "2030-01-08T09:45:00Z", "2030-01-08T10:15:00Z", "CONFIRMED"));

		assertThat(overlap).isInstanceOf(DataIntegrityViolationException.class);
		assertThat(BookingService.isOverlap(overlap)).isTrue();
	}

	private void insertBooking(UUID space, UUID user, String start, String end, String status) {
		jdbc.update("""
				insert into bookings (id, space_id, user_id, title, starts_at, ends_at, status, created_at)
				values (?, ?, ?, 'raw', ?, ?, ?, now())""", UUID.randomUUID(), space, user,
				Timestamp.from(Instant.parse(start)), Timestamp.from(Instant.parse(end)), status);
	}

}
