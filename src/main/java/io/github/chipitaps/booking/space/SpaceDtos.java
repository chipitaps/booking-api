package io.github.chipitaps.booking.space;

import io.github.chipitaps.booking.common.TimeRange;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class SpaceDtos {

	private SpaceDtos() {
	}

	public record SpaceRequest(
			@NotBlank(message = "Name the space.") @Size(max = 80, message = "Use at most 80 characters.") String name,
			@Size(max = 500, message = "Use at most 500 characters.") String description,
			@NotNull(message = "Say how many people fit.") @Min(value = 1, message = "At least 1 person.") @Max(value = 500, message = "At most 500 people.") Integer capacity,
			@NotBlank(message = "Give a time zone such as America/Bogota.") String timeZone,
			@NotNull(message = "Give an opening time.") LocalTime opensAt,
			@NotNull(message = "Give a closing time.") LocalTime closesAt) {
	}

	public record SpaceResponse(UUID id, String name, String description, int capacity, String timeZone,
			LocalTime opensAt, LocalTime closesAt, boolean active) {

		public static SpaceResponse of(Space space) {
			return new SpaceResponse(space.getId(), space.getName(), space.getDescription(), space.getCapacity(),
					space.getZone().getId(), space.getOpensAt(), space.getClosesAt(), space.isActive());
		}

	}

	public record Slot(OffsetDateTime startsAt, OffsetDateTime endsAt) {

		public static Slot of(TimeRange range, ZoneId zone) {
			return new Slot(range.start().atZone(zone).toOffsetDateTime(), range.end().atZone(zone).toOffsetDateTime());
		}

	}

	public record AvailabilityResponse(UUID spaceId, LocalDate date, String timeZone, LocalTime opensAt,
			LocalTime closesAt, List<Slot> free, List<Slot> booked) {
	}

}
