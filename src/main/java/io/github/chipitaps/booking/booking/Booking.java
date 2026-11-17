package io.github.chipitaps.booking.booking;

import io.github.chipitaps.booking.common.TimeRange;
import io.github.chipitaps.booking.space.Space;
import io.github.chipitaps.booking.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id")
	private Space space;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private AppUser user;

	private String title;

	private Instant startsAt;

	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	private BookingStatus status;

	private Instant createdAt;

	private Instant cancelledAt;

	protected Booking() {
	}

	public Booking(Space space, AppUser user, String title, TimeRange range, Instant createdAt) {
		this.space = space;
		this.user = user;
		this.title = title;
		this.startsAt = range.start();
		this.endsAt = range.end();
		this.status = BookingStatus.CONFIRMED;
		this.createdAt = createdAt;
	}

	public void cancel(Instant at) {
		this.status = BookingStatus.CANCELLED;
		this.cancelledAt = at;
	}

	public boolean isOwnedBy(UUID userId) {
		return user.getId().equals(userId);
	}

	public TimeRange range() {
		return new TimeRange(startsAt, endsAt);
	}

	public UUID getId() {
		return id;
	}

	public Space getSpace() {
		return space;
	}

	public String getTitle() {
		return title;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

}
