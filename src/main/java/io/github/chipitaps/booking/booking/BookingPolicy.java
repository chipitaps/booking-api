package io.github.chipitaps.booking.booking;

import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.Space;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BookingPolicy {

	public static final Duration MIN_LENGTH = Duration.ofMinutes(15);

	public static final Duration MAX_LENGTH = Duration.ofHours(4);

	public static final Duration MAX_AHEAD = Duration.ofDays(90);

	public static final int MAX_UPCOMING_PER_USER = 5;

	private BookingPolicy() {
	}

	public static void check(Space space, TimeRange range, Instant now) {
		if (!space.isActive()) {
			throw ApiException.unprocessable("This space is not taking bookings.");
		}
		ZoneId zone = space.getZone();
		ZonedDateTime start = range.start().atZone(zone);
		ZonedDateTime end = range.end().atZone(zone);
		if (!onQuarterHour(start) || !onQuarterHour(end)) {
			throw ApiException.unprocessable("Bookings start and end on a quarter hour, like 09:00 or 09:15.");
		}
		Duration length = range.duration();
		if (length.compareTo(MIN_LENGTH) < 0 || length.compareTo(MAX_LENGTH) > 0) {
			throw ApiException.unprocessable("A booking lasts between 15 minutes and 4 hours.");
		}
		if (!range.start().isAfter(now)) {
			throw ApiException.unprocessable("That time has already started or passed.");
		}
		if (range.start().isAfter(now.plus(MAX_AHEAD))) {
			throw ApiException.unprocessable("Bookings can be made up to 90 days ahead.");
		}
		boolean sameDay = start.toLocalDate().equals(end.toLocalDate());
		boolean withinHours = !start.toLocalTime().isBefore(space.getOpensAt())
				&& !end.toLocalTime().isAfter(space.getClosesAt());
		if (!sameDay || !withinHours) {
			throw ApiException.unprocessable("This space is open from %s to %s (%s).".formatted(space.getOpensAt(),
					space.getClosesAt(), zone.getId()));
		}
	}

	private static boolean onQuarterHour(ZonedDateTime time) {
		return time.getMinute() % 15 == 0 && time.getSecond() == 0 && time.getNano() == 0;
	}

}
