package io.github.chipitaps.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.Space;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class BookingPolicyTest {

	private static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");

	private static Space space(String zone) {
		return new Space("Room", "", 4, ZoneId.of(zone), LocalTime.of(8, 0), LocalTime.of(20, 0), NOW);
	}

	private static TimeRange range(String start, String end) {
		return new TimeRange(Instant.parse(start), Instant.parse(end));
	}

	private static void rejects(Space space, TimeRange range, String message) {
		assertThatThrownBy(() -> BookingPolicy.check(space, range, NOW)).isInstanceOf(ApiException.class)
			.hasMessageContaining(message);
	}

	@Test
	void acceptsAQuarterAlignedBookingInsideOpeningHours() {
		assertThatCode(() -> BookingPolicy.check(space("UTC"), range("2030-01-08T09:15:00Z", "2030-01-08T10:45:00Z"),
				NOW))
			.doesNotThrowAnyException();
	}

	@Test
	void acceptsABookingThatFillsTheWholeLastSlot() {
		assertThatCode(() -> BookingPolicy.check(space("UTC"), range("2030-01-08T16:00:00Z", "2030-01-08T20:00:00Z"),
				NOW))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsTimesOffTheQuarterHour() {
		rejects(space("UTC"), range("2030-01-08T09:10:00Z", "2030-01-08T10:00:00Z"), "quarter hour");
		rejects(space("UTC"), range("2030-01-08T09:00:00Z", "2030-01-08T10:00:30Z"), "quarter hour");
	}

	@Test
	void rejectsBookingsShorterThan15MinutesOrLongerThan4Hours() {
		rejects(space("UTC"), range("2030-01-08T09:00:00Z", "2030-01-08T13:15:00Z"), "between 15 minutes and 4 hours");
	}

	@Test
	void rejectsTimeThatHasStartedOrIsTooFarAhead() {
		rejects(space("UTC"), range("2030-01-07T12:00:00Z", "2030-01-07T13:00:00Z"), "already started");
		rejects(space("UTC"), range("2030-01-07T09:00:00Z", "2030-01-07T10:00:00Z"), "already started");
		rejects(space("UTC"), range("2030-04-08T09:00:00Z", "2030-04-08T10:00:00Z"), "90 days");
	}

	@Test
	void rejectsTimeOutsideOpeningHours() {
		rejects(space("UTC"), range("2030-01-08T07:00:00Z", "2030-01-08T09:00:00Z"), "open from 08:00 to 20:00 (UTC)");
		rejects(space("UTC"), range("2030-01-08T19:00:00Z", "2030-01-08T20:15:00Z"), "open from 08:00 to 20:00");
	}

	@Test
	void appliesOpeningHoursInTheSpacesTimeZone() {
		Space bogota = space("America/Bogota");

		assertThatCode(() -> BookingPolicy.check(bogota, range("2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z"), NOW))
			.doesNotThrowAnyException();
		rejects(bogota, range("2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"), "(America/Bogota)");
	}

	@Test
	void checksQuarterHoursInLocalTimeForZonesWithOddOffsets() {
		Space kolkata = new Space("Room", "", 4, ZoneId.of("Asia/Kolkata"), LocalTime.of(0, 0), LocalTime.of(23, 45),
				NOW);

		assertThatCode(() -> BookingPolicy.check(kolkata, range("2030-01-08T03:30:00Z", "2030-01-08T04:30:00Z"), NOW))
			.doesNotThrowAnyException();
		rejects(kolkata, range("2030-01-08T03:20:00Z", "2030-01-08T04:20:00Z"), "quarter hour");
	}

	@Test
	void rejectsSpacesThatStoppedTakingBookings() {
		Space closed = space("UTC");
		closed.deactivate();

		rejects(closed, range("2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"), "not taking bookings");
	}

	@Test
	void recognisesTheExclusionConstraintViolationAnywhereInTheCauseChain() {
		SQLException overlap = new SQLException("conflicting key value violates exclusion constraint", "23P01");
		SQLException unique = new SQLException("duplicate key value violates unique constraint", "23505");

		assertThat(BookingService.isOverlap(new DataIntegrityViolationException("x", new RuntimeException(overlap))))
			.isTrue();
		assertThat(BookingService.isOverlap(new DataIntegrityViolationException("x", unique))).isFalse();
		assertThat(BookingService.isOverlap(new DataIntegrityViolationException("x"))).isFalse();
	}

}
