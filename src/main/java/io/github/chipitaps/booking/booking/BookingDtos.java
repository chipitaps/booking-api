package io.github.chipitaps.booking.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public final class BookingDtos {

	private BookingDtos() {
	}

	public record BookingRequest(@NotNull(message = "Choose a space.") UUID spaceId,
			@NotBlank(message = "Give the booking a title.") @Size(max = 120, message = "Use at most 120 characters.") String title,
			@NotNull(message = "Give a start time.") OffsetDateTime startsAt,
			@NotNull(message = "Give an end time.") OffsetDateTime endsAt) {
	}

	public record SpaceSummary(UUID id, String name, String timeZone) {
	}

	public record BookingResponse(UUID id, SpaceSummary space, String title, OffsetDateTime startsAt,
			OffsetDateTime endsAt, BookingStatus status, Instant createdAt, Instant cancelledAt) {

		public static BookingResponse of(Booking booking) {
			ZoneId zone = booking.getSpace().getZone();
			return new BookingResponse(booking.getId(),
					new SpaceSummary(booking.getSpace().getId(), booking.getSpace().getName(), zone.getId()),
					booking.getTitle(), booking.getStartsAt().atZone(zone).toOffsetDateTime(),
					booking.getEndsAt().atZone(zone).toOffsetDateTime(), booking.getStatus(), booking.getCreatedAt(),
					booking.getCancelledAt());
		}

	}

}
