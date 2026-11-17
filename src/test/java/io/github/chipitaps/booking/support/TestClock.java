package io.github.chipitaps.booking.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public class TestClock extends Clock {

	private final AtomicReference<Instant> now;

	public TestClock(Instant start) {
		this.now = new AtomicReference<>(start);
	}

	public void set(Instant instant) {
		now.set(instant);
	}

	public void advance(Duration duration) {
		now.updateAndGet((current) -> current.plus(duration));
	}

	@Override
	public Instant instant() {
		return now.get();
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return Clock.fixed(instant(), zone);
	}

}
