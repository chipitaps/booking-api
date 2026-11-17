package io.github.chipitaps.booking.auth;

import io.github.chipitaps.booking.user.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegisterRequest(
			@NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") @Size(max = 254) String email,
			@NotBlank(message = "Enter your name.") @Size(max = 80, message = "Use at most 80 characters.") String name,
			@NotBlank(message = "Choose a password.") @Size(min = 10, max = 72, message = "Use between 10 and 72 characters.") String password) {

		public RegisterRequest {
			email = normalizeEmail(email);
		}

	}

	public record LoginRequest(@NotBlank(message = "Enter your email.") String email,
			@NotBlank(message = "Enter your password.") String password) {

		public LoginRequest {
			email = normalizeEmail(email);
		}

	}

	static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	public record UserResponse(UUID id, String email, String name, String role) {

		public static UserResponse of(AppUser user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
		}

	}

	public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

		public static TokenResponse of(JwtService.IssuedToken token, AppUser user) {
			return new TokenResponse(token.value(), "Bearer", token.expiresAt(), UserResponse.of(user));
		}

	}

}
