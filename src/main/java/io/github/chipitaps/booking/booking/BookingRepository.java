package io.github.chipitaps.booking.booking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@EntityGraph(attributePaths = "space")
	Optional<Booking> findWithSpaceById(UUID id);

	@EntityGraph(attributePaths = "space")
	Page<Booking> findByUserIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(UUID userId, BookingStatus status,
			Instant after, Pageable pageable);

	@EntityGraph(attributePaths = "space")
	Page<Booking> findByUserIdOrderByStartsAtDesc(UUID userId, Pageable pageable);

	long countByUserIdAndStatusAndEndsAtAfter(UUID userId, BookingStatus status, Instant after);

	@Query("""
			select b from Booking b
			where b.space.id = :spaceId and b.status = :status and b.startsAt < :to and b.endsAt > :from
			order by b.startsAt""")
	List<Booking> findOverlapping(UUID spaceId, BookingStatus status, Instant from, Instant to);

	default List<Booking> findConfirmedOverlapping(UUID spaceId, Instant from, Instant to) {
		return findOverlapping(spaceId, BookingStatus.CONFIRMED, from, to);
	}

}
