package io.github.chipitaps.booking.booking;

import io.github.chipitaps.booking.booking.BookingDtos.BookingRequest;
import io.github.chipitaps.booking.booking.BookingDtos.BookingResponse;
import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.common.Caller;
import io.github.chipitaps.booking.common.PageResponse;
import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.Space;
import io.github.chipitaps.booking.space.SpaceRepository;
import io.github.chipitaps.booking.user.AppUser;
import io.github.chipitaps.booking.user.UserRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

	static final String EXCLUSION_VIOLATION = "23P01";

	private final BookingRepository bookings;

	private final SpaceRepository spaces;

	private final UserRepository users;

	private final Clock clock;

	BookingService(BookingRepository bookings, SpaceRepository spaces, UserRepository users, Clock clock) {
		this.bookings = bookings;
		this.spaces = spaces;
		this.users = users;
		this.clock = clock;
	}

	@Transactional
	public BookingResponse create(Caller caller, BookingRequest request) {
		AppUser user = users.lockById(caller.id())
			.orElseThrow(() -> ApiException.unauthorized("This account no longer exists."));
		Space space = spaces.findById(request.spaceId())
			.orElseThrow(() -> ApiException.notFound("There is no space with that id."));
		Instant start = request.startsAt().toInstant();
		Instant end = request.endsAt().toInstant();
		if (!end.isAfter(start)) {
			throw ApiException.unprocessable("The booking must end after it starts.");
		}
		TimeRange range = new TimeRange(start, end);
		Instant now = clock.instant();
		BookingPolicy.check(space, range, now);

		long upcoming = bookings.countByUserIdAndStatusAndEndsAtAfter(user.getId(), BookingStatus.CONFIRMED, now);
		if (upcoming >= BookingPolicy.MAX_UPCOMING_PER_USER) {
			throw ApiException.unprocessable("You already have %d upcoming bookings. Cancel one before adding another."
				.formatted(BookingPolicy.MAX_UPCOMING_PER_USER));
		}

		Booking booking = new Booking(space, user, request.title().trim(), range, now);
		try {
			bookings.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException exception) {
			if (isOverlap(exception)) {
				throw ApiException.conflict("This space is already booked for part of that time.");
			}
			throw exception;
		}
		return BookingResponse.of(booking);
	}

	@Transactional(readOnly = true)
	public BookingResponse get(Caller caller, UUID id) {
		return BookingResponse.of(findVisible(caller, id));
	}

	@Transactional(readOnly = true)
	public PageResponse<BookingResponse> list(Caller caller, String scope, Pageable pageable) {
		Page<Booking> page = switch (scope) {
			case "upcoming" -> bookings.findByUserIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(caller.id(),
					BookingStatus.CONFIRMED, clock.instant(), pageable);
			case "all" -> bookings.findByUserIdOrderByStartsAtDesc(caller.id(), pageable);
			default -> throw ApiException.unprocessable("scope must be \"upcoming\" or \"all\".");
		};
		return PageResponse.of(page, BookingResponse::of);
	}

	@Transactional
	public BookingResponse cancel(Caller caller, UUID id) {
		Booking booking = findVisible(caller, id);
		Instant now = clock.instant();
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			throw ApiException.conflict("This booking is already cancelled.");
		}
		if (!booking.getStartsAt().isAfter(now)) {
			throw ApiException.conflict("This booking has already started and can no longer be cancelled.");
		}
		booking.cancel(now);
		return BookingResponse.of(booking);
	}

	private Booking findVisible(Caller caller, UUID id) {
		return bookings.findWithSpaceById(id)
			.filter((booking) -> caller.admin() || booking.isOwnedBy(caller.id()))
			.orElseThrow(() -> ApiException.notFound("There is no booking with that id."));
	}

	static boolean isOverlap(Throwable exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

}
