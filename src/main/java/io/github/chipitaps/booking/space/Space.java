package io.github.chipitaps.booking.space;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "spaces")
public class Space {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	private String description;

	private int capacity;

	private String timeZone;

	private LocalTime opensAt;

	private LocalTime closesAt;

	private boolean active;

	private Instant createdAt;

	protected Space() {
	}

	public Space(String name, String description, int capacity, ZoneId timeZone, LocalTime opensAt,
			LocalTime closesAt, Instant createdAt) {
		update(name, description, capacity, timeZone, opensAt, closesAt);
		this.active = true;
		this.createdAt = createdAt;
	}

	public final void update(String name, String description, int capacity, ZoneId timeZone, LocalTime opensAt,
			LocalTime closesAt) {
		this.name = name;
		this.description = description;
		this.capacity = capacity;
		this.timeZone = timeZone.getId();
		this.opensAt = opensAt;
		this.closesAt = closesAt;
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getCapacity() {
		return capacity;
	}

	public ZoneId getZone() {
		return ZoneId.of(timeZone);
	}

	public LocalTime getOpensAt() {
		return opensAt;
	}

	public LocalTime getClosesAt() {
		return closesAt;
	}

	public boolean isActive() {
		return active;
	}

}
