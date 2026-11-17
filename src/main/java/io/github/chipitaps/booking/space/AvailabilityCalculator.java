package io.github.chipitaps.booking.space;

import io.github.chipitaps.booking.common.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AvailabilityCalculator {

	public static final Duration SLOT = Duration.ofMinutes(15);

	private AvailabilityCalculator() {
	}

	public static TimeRange openingWindow(LocalDate date, ZoneId zone, LocalTime opensAt, LocalTime closesAt) {
		return new TimeRange(date.atTime(opensAt).atZone(zone).toInstant(),
				date.atTime(closesAt).atZone(zone).toInstant());
	}

	public static List<TimeRange> freeRanges(TimeRange window, List<TimeRange> booked, Instant now) {
		Instant cursor = later(window.start(), roundUpToSlot(now));
		List<TimeRange> free = new ArrayList<>();
		List<TimeRange> ordered = booked.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
		for (TimeRange range : ordered) {
			if (!range.start().isBefore(window.end())) {
				break;
			}
			if (!range.end().isAfter(cursor)) {
				continue;
			}
			if (range.start().isAfter(cursor)) {
				free.add(new TimeRange(cursor, range.start()));
			}
			cursor = later(cursor, range.end());
		}
		if (cursor.isBefore(window.end())) {
			free.add(new TimeRange(cursor, window.end()));
		}
		return free;
	}

	public static Instant roundUpToSlot(Instant instant) {
		long slotSeconds = SLOT.toSeconds();
		long seconds = instant.getEpochSecond();
		boolean aligned = seconds % slotSeconds == 0 && instant.getNano() == 0;
		long rounded = aligned ? seconds : (Math.floorDiv(seconds, slotSeconds) + 1) * slotSeconds;
		return Instant.ofEpochSecond(rounded);
	}

	private static Instant later(Instant a, Instant b) {
		return a.isAfter(b) ? a : b;
	}

}
