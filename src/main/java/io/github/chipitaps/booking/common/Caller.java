package io.github.chipitaps.booking.common;

import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record Caller(UUID id, boolean admin) {

	public static Caller of(JwtAuthenticationToken token) {
		boolean admin = token.getAuthorities()
			.stream()
			.anyMatch((authority) -> "ROLE_ADMIN".equals(authority.getAuthority()));
		return new Caller(UUID.fromString(token.getToken().getSubject()), admin);
	}

}
