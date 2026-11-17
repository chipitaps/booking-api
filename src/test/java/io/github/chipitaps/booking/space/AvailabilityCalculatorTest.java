package io.github.chipitaps.booking.space;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chipitaps.booking.common.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

	private static final TimeRange DAY = range("2030-01-08T08:00:00Z", "2030-01-08T20:00:00Z");

	private static final Instant EARLIER = Instant.parse("2030-01-01T00:00:00Z");

	private static TimeRange range(String start, String end) {
		return new TimeRange(Instant.parse(start), Instant.parse(end));
	}

	@Test
	void anEmptyDayIsOneFreeRange() {
		assertThat(AvailabilityCalculator.freeRanges(DAY, List.of(), EARLIER)).containsExactly(DAY);
	}

	@Test
	void bookingsAreCutOutOfTheDay() {
		List<TimeRange> booked = List.of(range("2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z"),
				range("2030-01-08T09:00:00Z", "2030-01-08T10:30:00Z"));

		assertThat(AvailabilityCalculator.freeRanges(DAY, booked, EARLIER)).containsExactly(
				range("2030-01-08T08:00:00Z", "2030-01-08T09:00:00Z"),
				range("2030-01-08T10:30:00Z", "2030-01-08T13:00:00Z"),
				range("2030-01-08T14:00:00Z", "2030-01-08T20:00:00Z"));
	}

	@Test
	void backToBackBookingsLeaveNoGap() {
		List<TimeRange> booked = List.of(range("2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"),
				range("2030-01-08T10:00:00Z", "2030-01-08T11:00:00Z"));

		assertThat(AvailabilityCalculator.freeRanges(DAY, booked, EARLIER)).containsExactly(
				range("2030-01-08T08:00:00Z", "2030-01-08T09:00:00Z"),
				range("2030-01-08T11:00:00Z", "2030-01-08T20:00:00Z"));
	}

	@Test
	void bookingsReachingPastTheWindowAreClipped() {
		List<TimeRange> booked = List.of(range("2030-01-08T06:00:00Z", "2030-01-08T08:30:00Z"),
				range("2030-01-08T19:00:00Z", "2030-01-08T23:00:00Z"));

		assertThat(AvailabilityCalculator.freeRanges(DAY, booked, EARLIER))
			.containsExactly(range("2030-01-08T08:30:00Z", "2030-01-08T19:00:00Z"));
	}

	@Test
	void aFullyBookedDayHasNothingFree() {
		assertThat(AvailabilityCalculator.freeRanges(DAY, List.of(DAY), EARLIER)).isEmpty();
	}

	@Test
	void timeThatHasPassedIsNotFreeAndTheRestStartsOnTheNextSlot() {
		Instant now = Instant.parse("2030-01-08T11:07:30Z");

		assertThat(AvailabilityCalculator.freeRanges(DAY, List.of(), now))
			.containsExactly(range("2030-01-08T11:15:00Z", "2030-01-08T20:00:00Z"));
		assertThat(AvailabilityCalculator.freeRanges(DAY, List.of(), Instant.parse("2030-01-08T21:00:00Z"))).isEmpty();
	}

	@Test
	void roundsUpToTheNextQuarterHourOnlyWhenNeeded() {
		assertThat(AvailabilityCalculator.roundUpToSlot(Instant.parse("2030-01-08T11:15:00Z")))
			.isEqualTo(Instant.parse("2030-01-08T11:15:00Z"));
		assertThat(AvailabilityCalculator.roundUpToSlot(Instant.parse("2030-01-08T11:15:00.001Z")))
			.isEqualTo(Instant.parse("2030-01-08T11:30:00Z"));
		assertThat(AvailabilityCalculator.roundUpToSlot(Instant.parse("2030-01-08T11:59:59Z")))
			.isEqualTo(Instant.parse("2030-01-08T12:00:00Z"));
	}

	@Test
	void openingHoursFollowTheSpacesTimeZoneAcrossDaylightSavingChanges() {
		ZoneId newYork = ZoneId.of("America/New_York");
		LocalTime opens = LocalTime.of(8, 0);
		LocalTime closes = LocalTime.of(20, 0);

		TimeRange beforeChange = AvailabilityCalculator.openingWindow(LocalDate.of(2030, 3, 9), newYork, opens, closes);
		TimeRange afterChange = AvailabilityCalculator.openingWindow(LocalDate.of(2030, 3, 10), newYork, opens, closes);

		assertThat(beforeChange).isEqualTo(range("2030-03-09T13:00:00Z", "2030-03-10T01:00:00Z"));
		assertThat(afterChange).isEqualTo(range("2030-03-10T12:00:00Z", "2030-03-11T00:00:00Z"));
	}

	@Test
	void utcWindowIsTheLocalTimes() {
		assertThat(AvailabilityCalculator.openingWindow(LocalDate.of(2030, 1, 8), ZoneOffset.UTC, LocalTime.of(8, 0),
				LocalTime.of(20, 0)))
			.isEqualTo(DAY);
	}

}
