package io.github.chipitaps.booking.auth;

import io.github.chipitaps.booking.auth.AuthDtos.LoginRequest;
import io.github.chipitaps.booking.auth.AuthDtos.RegisterRequest;
import io.github.chipitaps.booking.auth.AuthDtos.TokenResponse;
import io.github.chipitaps.booking.auth.AuthDtos.UserResponse;
import io.github.chipitaps.booking.common.ApiException;
import io.github.chipitaps.booking.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Accounts")
class AuthController {

	private final AuthService authService;

	private final UserRepository users;

	AuthController(AuthService authService, UserRepository users) {
		this.authService = authService;
		this.users = users;
	}

	@PostMapping("/api/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an account and receive an access token")
	TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/api/auth/login")
	@Operation(summary = "Exchange email and password for an access token")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/api/me")
	@Operation(summary = "The account behind the current token")
	@SecurityRequirement(name = "bearer")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return users.findById(UUID.fromString(jwt.getSubject()))
			.map(UserResponse::of)
			.orElseThrow(() -> ApiException.unauthorized("This account no longer exists."));
	}

}
