package io.github.chipitaps.booking.space;

import io.github.chipitaps.booking.space.SpaceDtos.AvailabilityResponse;
import io.github.chipitaps.booking.space.SpaceDtos.SpaceRequest;
import io.github.chipitaps.booking.space.SpaceDtos.SpaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spaces")
@Tag(name = "Spaces")
class SpaceController {

	private final SpaceService service;

	SpaceController(SpaceService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Spaces that are taking bookings")
	List<SpaceResponse> list() {
		return service.listActive();
	}

	@GetMapping("/{id}")
	@Operation(summary = "One space")
	SpaceResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@GetMapping("/{id}/availability")
	@Operation(summary = "Free and booked time on a date, in the space's time zone")
	AvailabilityResponse availability(@PathVariable UUID id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return service.availability(id, date);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Add a space (admins only)")
	@SecurityRequirement(name = "bearer")
	ResponseEntity<SpaceResponse> create(@Valid @RequestBody SpaceRequest request) {
		SpaceResponse space = service.create(request);
		return ResponseEntity.created(URI.create("/api/spaces/" + space.id())).body(space);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Change a space (admins only). Existing bookings are kept.")
	@SecurityRequirement(name = "bearer")
	SpaceResponse update(@PathVariable UUID id, @Valid @RequestBody SpaceRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Stop taking bookings for a space (admins only). Existing bookings are kept.")
	@SecurityRequirement(name = "bearer")
	void deactivate(@PathVariable UUID id) {
		service.deactivate(id);
	}

}
