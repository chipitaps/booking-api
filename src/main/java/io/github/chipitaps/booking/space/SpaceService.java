package io.github.chipitaps.booking.space;

import io.github.chipitaps.booking.booking.BookingRepository;
import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.SpaceDtos.AvailabilityResponse;
import io.github.chipitaps.booking.space.SpaceDtos.Slot;
import io.github.chipitaps.booking.space.SpaceDtos.SpaceRequest;
import io.github.chipitaps.booking.space.SpaceDtos.SpaceResponse;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpaceService {

	private final SpaceRepository spaces;

	private final BookingRepository bookings;

	private final Clock clock;

	SpaceService(SpaceRepository spaces, BookingRepository bookings, Clock clock) {
		this.spaces = spaces;
		this.bookings = bookings;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<SpaceResponse> listActive() {
		return spaces.findByActiveTrueOrderByNameAsc().stream().map(SpaceResponse::of).toList();
	}

	@Transactional(readOnly = true)
	public SpaceResponse get(UUID id) {
		return SpaceResponse.of(find(id));
	}

	@Transactional
	public SpaceResponse create(SpaceRequest request) {
		String name = request.name().trim();
		if (spaces.existsByNameIgnoreCase(name)) {
			throw ApiException.conflict("A space called \"" + name + "\" already exists.");
		}
		ZoneId zone = zone(request.timeZone());
		checkHours(request.opensAt(), request.closesAt());
		Space space = new Space(name, description(request), request.capacity(), zone, request.opensAt(),
				request.closesAt(), clock.instant());
		return SpaceResponse.of(spaces.save(space));
	}

	@Transactional
	public SpaceResponse update(UUID id, SpaceRequest request) {
		Space space = find(id);
		String name = request.name().trim();
		if (spaces.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw ApiException.conflict("A space called \"" + name + "\" already exists.");
		}
		ZoneId zone = zone(request.timeZone());
		checkHours(request.opensAt(), request.closesAt());
		space.update(name, description(request), request.capacity(), zone, request.opensAt(), request.closesAt());
		return SpaceResponse.of(space);
	}

	@Transactional
	public void deactivate(UUID id) {
		find(id).deactivate();
	}

	@Transactional(readOnly = true)
	public AvailabilityResponse availability(UUID id, LocalDate date) {
		Space space = find(id);
		ZoneId zone = space.getZone();
		TimeRange window = AvailabilityCalculator.openingWindow(date, zone, space.getOpensAt(), space.getClosesAt());
		List<TimeRange> booked = bookings.findConfirmedOverlapping(space.getId(), window.start(), window.end())
			.stream()
			.map((booking) -> booking.range())
			.toList();
		List<TimeRange> free = space.isActive() ? AvailabilityCalculator.freeRanges(window, booked, clock.instant())
				: List.of();
		return new AvailabilityResponse(space.getId(), date, zone.getId(), space.getOpensAt(), space.getClosesAt(),
				free.stream().map((range) -> Slot.of(range, zone)).toList(),
				booked.stream().map((range) -> Slot.of(range, zone)).toList());
	}

	Space find(UUID id) {
		return spaces.findById(id).orElseThrow(() -> ApiException.notFound("There is no space with that id."));
	}

	private static String description(SpaceRequest request) {
		return request.description() == null ? "" : request.description().trim();
	}

	private static ZoneId zone(String value) {
		try {
			return ZoneId.of(value.trim());
		}
		catch (DateTimeException exception) {
			throw ApiException.unprocessable("\"" + value + "\" is not a known time zone. Use a name like America/Bogota.");
		}
	}

	private static void checkHours(LocalTime opensAt, LocalTime closesAt) {
		if (!opensAt.isBefore(closesAt)) {
			throw ApiException.unprocessable("The closing time must be after the opening time.");
		}
		if (opensAt.getMinute() % 15 != 0 || closesAt.getMinute() % 15 != 0 || opensAt.getSecond() != 0
				|| closesAt.getSecond() != 0) {
			throw ApiException.unprocessable("Opening hours must start and end on a quarter hour.");
		}
	}

}
