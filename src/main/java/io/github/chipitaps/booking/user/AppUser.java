package io.github.chipitaps.booking.user;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String email;

	private String name;

	private String passwordHash;

	@Enumerated(EnumType.STRING)
	private Role role;

	private Instant createdAt;

	protected AppUser() {
	}

	public AppUser(String email, String name, String passwordHash, Role role, Instant createdAt) {
		this.email = email;
		this.name = name;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
