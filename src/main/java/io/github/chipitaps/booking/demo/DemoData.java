package io.github.chipitaps.booking.demo;

import io.github.chipitaps.booking.booking.Booking;
import io.github.chipitaps.booking.booking.BookingRepository;
import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.Space;
import io.github.chipitaps.booking.space.SpaceRepository;
import io.github.chipitaps.booking.user.AppUser;
import io.github.chipitaps.booking.user.Role;
import io.github.chipitaps.booking.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnBooleanProperty("app.demo-data")
class DemoData implements ApplicationRunner {

	static final String ADMIN_EMAIL = "admin@example.com";

	static final String ADMIN_PASSWORD = "admin-demo-password";

	static final String USER_EMAIL = "demo@example.com";

	static final String USER_PASSWORD = "demo-password";

	private static final Logger log = LoggerFactory.getLogger(DemoData.class);

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	private final UserRepository users;

	private final SpaceRepository spaces;

	private final BookingRepository bookings;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	DemoData(UserRepository users, SpaceRepository spaces, BookingRepository bookings,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.users = users;
		this.spaces = spaces;
		this.bookings = bookings;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			return;
		}
		Instant now = clock.instant();
		users.save(new AppUser(ADMIN_EMAIL, "Admin", passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN, now));
		AppUser demo = users
			.save(new AppUser(USER_EMAIL, "Demo User", passwordEncoder.encode(USER_PASSWORD), Role.USER, now));

		Space focus = spaces.save(new Space("Focus Room", "Quiet room with a whiteboard and a screen.", 4, BOGOTA,
				LocalTime.of(7, 0), LocalTime.of(21, 0), now));
		Space board = spaces.save(new Space("Board Room", "Large table, video conferencing kit.", 12, BOGOTA,
				LocalTime.of(8, 0), LocalTime.of(19, 0), now));
		spaces.save(new Space("Phone Booth", "Soundproof booth for calls.", 1, BOGOTA, LocalTime.of(7, 0),
				LocalTime.of(21, 0), now));
		spaces.save(new Space("Rooftop Terrace", "Open-air space for events.", 40, BOGOTA, LocalTime.of(10, 0),
				LocalTime.of(18, 0), now));

		LocalDate tomorrow = LocalDate.now(clock.withZone(BOGOTA)).plusDays(1);
		bookings.save(new Booking(focus, demo, "Sprint planning", at(tomorrow, 9, 0, 10, 30), now));
		bookings.save(new Booking(board, demo, "Quarterly review", at(tomorrow, 14, 0, 16, 0), now));
		log.info("Demo data created: log in as {} or {}", USER_EMAIL, ADMIN_EMAIL);
	}

	private static TimeRange at(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
		return new TimeRange(date.atTime(startHour, startMinute).atZone(BOGOTA).toInstant(),
				date.atTime(endHour, endMinute).atZone(BOGOTA).toInstant());
	}

}
