package io.github.chipitaps.booking.booking;

import io.github.chipitaps.booking.booking.BookingDtos.BookingRequest;
import io.github.chipitaps.booking.booking.BookingDtos.BookingResponse;
import io.github.chipitaps.booking.common.Caller;
import io.github.chipitaps.booking.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
@SecurityRequirement(name = "bearer")
class BookingController {

	private final BookingService service;

	BookingController(BookingService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Book a space. Answers 409 if any part of the time is already taken.")
	ResponseEntity<BookingResponse> create(JwtAuthenticationToken token, @Valid @RequestBody BookingRequest request) {
		BookingResponse booking = service.create(Caller.of(token), request);
		return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
	}

	@GetMapping
	@Operation(summary = "Your bookings")
	PageResponse<BookingResponse> list(JwtAuthenticationToken token,
			@Parameter(description = "upcoming or all") @RequestParam(defaultValue = "upcoming") String scope,
			@PageableDefault(size = 20) Pageable pageable) {
		return service.list(Caller.of(token), scope, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "One of your bookings (admins can see any)")
	BookingResponse get(JwtAuthenticationToken token, @PathVariable UUID id) {
		return service.get(Caller.of(token), id);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel a booking that has not started yet")
	BookingResponse cancel(JwtAuthenticationToken token, @PathVariable UUID id) {
		return service.cancel(Caller.of(token), id);
	}

}
