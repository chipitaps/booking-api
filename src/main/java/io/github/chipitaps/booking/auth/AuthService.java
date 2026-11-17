package io.github.chipitaps.booking.auth;

import io.github.chipitaps.booking.auth.AuthDtos.LoginRequest;
import io.github.chipitaps.booking.auth.AuthDtos.RegisterRequest;
import io.github.chipitaps.booking.auth.AuthDtos.TokenResponse;
import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.user.AppUser;
import io.github.chipitaps.booking.user.Role;
import io.github.chipitaps.booking.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final LoginAttempts loginAttempts;

	private final Clock clock;

	private final String timingDummyHash;

	AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			LoginAttempts loginAttempts, Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.loginAttempts = loginAttempts;
		this.clock = clock;
		this.timingDummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		String email = request.email();
		if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
			throw ApiException.unprocessable("The password is too long.");
		}
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("An account with that email already exists.");
		}
		AppUser user = users.save(new AppUser(email, request.name().trim(),
				passwordEncoder.encode(request.password()), Role.USER, clock.instant()));
		return TokenResponse.of(jwtService.issue(user), user);
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		String email = request.email();
		if (loginAttempts.isBlocked(email)) {
			throw ApiException.tooManyRequests("Too many failed attempts. Try again in 15 minutes.");
		}
		Optional<AppUser> user = users.findByEmail(email);
		boolean valid = passwordEncoder.matches(request.password(),
				user.map(AppUser::getPasswordHash).orElse(timingDummyHash));
		if (user.isEmpty() || !valid) {
			loginAttempts.recordFailure(email);
			throw ApiException.unauthorized("Email or password is incorrect.");
		}
		loginAttempts.reset(email);
		return TokenResponse.of(jwtService.issue(user.get()), user.get());
	}

}
