package io.github.chipitaps.booking.common;

import java.time.Duration;
import java.time.Instant;

public record TimeRange(Instant start, Instant end) {

	public TimeRange {
		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("A time range must end after it starts.");
		}
	}

	public boolean overlaps(TimeRange other) {
		return start.isBefore(other.end) && other.start.isBefore(end);
	}

	public Duration duration() {
		return Duration.between(start, end);
	}

}
