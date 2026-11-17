package io.github.chipitaps.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chipitaps.booking.support.Api;
import io.github.chipitaps.booking.support.IntegrationTest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingIntegrationTest extends IntegrationTest {

	private String space;

	private String ana;

	private String ben;

	@BeforeEach
	void setUp() {
		space = createSpace("Focus Room");
		ana = register("Ana");
		ben = register("Ben");
	}

	@Test
	void bookingAFreeSlotSucceedsAndAnswersInTheSpacesTimeZone() {
		String bogota = createSpace(admin(), "Terrace", "America/Bogota", "08:00", "20:00");

		Api.Response response = book(ana, bogota, "2030-01-08T14:00:00Z", "2030-01-08T15:00:00Z");

		assertThat(response.status()).as(response.body()).isEqualTo(201);
		assertThat(response.location()).hasValue("/api/bookings/" + response.json("$.id"));
		assertThat(response.<String>json("$.status")).isEqualTo("CONFIRMED");
		assertThat(response.<String>json("$.space.name")).isEqualTo("Terrace");
		assertThat(OffsetDateTime.parse(response.json("$.startsAt")))
			.isEqualTo(OffsetDateTime.parse("2030-01-08T09:00:00-05:00"));
		assertThat(response.<String>json("$.startsAt")).endsWith("-05:00");
	}

	@Test
	void overlappingTimeIsRejectedWithAConflict() {
		assertThat(book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T11:00:00Z").status()).isEqualTo(201);

		Api.Response inside = book(ben, space, "2030-01-08T10:00:00Z", "2030-01-08T10:30:00Z");
		Api.Response straddling = book(ben, space, "2030-01-08T08:30:00Z", "2030-01-08T09:15:00Z");
		Api.Response covering = book(ben, space, "2030-01-08T08:00:00Z", "2030-01-08T12:00:00Z");

		assertThat(List.of(inside.status(), straddling.status(), covering.status())).containsOnly(409);
		assertThat(inside.<String>json("$.detail")).isEqualTo("This space is already booked for part of that time.");
	}

	@Test
	void backToBackBookingsAreAllowed() {
		assertThat(book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").status()).isEqualTo(201);
		assertThat(book(ben, space, "2030-01-08T10:00:00Z", "2030-01-08T11:00:00Z").status()).isEqualTo(201);
		assertThat(book(ben, space, "2030-01-08T08:00:00Z", "2030-01-08T09:00:00Z").status()).isEqualTo(201);
	}

	@Test
	void theSameTimeInAnotherSpaceIsFine() {
		String other = createSpace("Phone Booth");

		assertThat(book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").status()).isEqualTo(201);
		assertThat(book(ben, other, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").status()).isEqualTo(201);
	}

	@Test
	void cancellingFreesTheTimeForSomeoneElse() {
		String id = book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").json("$.id");

		Api.Response cancelled = api.post("/api/bookings/" + id + "/cancel", ana, null);

		assertThat(cancelled.status()).isEqualTo(200);
		assertThat(cancelled.<String>json("$.status")).isEqualTo("CANCELLED");
		assertThat(cancelled.<String>json("$.cancelledAt")).isNotNull();
		assertThat(book(ben, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").status()).isEqualTo(201);
	}

	@Test
	void aBookingCanOnlyBeCancelledOnceAndBeforeItStarts() {
		String id = book(ana, space, "2030-01-07T13:00:00Z", "2030-01-07T14:00:00Z").json("$.id");
		String later = book(ana, space, "2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z").json("$.id");

		clock.advance(Duration.ofHours(1));
		Api.Response started = api.post("/api/bookings/" + id + "/cancel", ana, null);
		assertThat(started.status()).isEqualTo(409);
		assertThat(started.<String>json("$.detail")).contains("already started");

		assertThat(api.post("/api/bookings/" + later + "/cancel", ana, null).status()).isEqualTo(200);
		assertThat(api.post("/api/bookings/" + later + "/cancel", ana, null).status()).isEqualTo(409);
	}

	@Test
	void otherUsersCannotSeeOrCancelYourBookingButAdminsCan() {
		String id = book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").json("$.id");
		String admin = admin();

		assertThat(api.get("/api/bookings/" + id, ben).status()).isEqualTo(404);
		assertThat(api.post("/api/bookings/" + id + "/cancel", ben, null).status()).isEqualTo(404);
		assertThat(api.get("/api/bookings/" + id, ana).status()).isEqualTo(200);
		assertThat(api.get("/api/bookings/" + id, admin).status()).isEqualTo(200);
		assertThat(api.post("/api/bookings/" + id + "/cancel", admin, null).status()).isEqualTo(200);
	}

	@Test
	void businessRulesAreExplained() {
		assertThat(rule("2030-01-07T09:00:00Z", "2030-01-07T10:00:00Z")).contains("already started");
		assertThat(rule("2030-01-08T09:10:00Z", "2030-01-08T10:00:00Z")).contains("quarter hour");
		assertThat(rule("2030-01-08T09:00:00Z", "2030-01-08T13:30:00Z")).contains("between 15 minutes and 4 hours");
		assertThat(rule("2030-01-08T06:00:00Z", "2030-01-08T07:00:00Z")).contains("open from 08:00 to 20:00 (UTC)");
		assertThat(rule("2030-01-08T10:00:00Z", "2030-01-08T09:00:00Z")).contains("must end after it starts");
		assertThat(rule("2030-06-01T09:00:00Z", "2030-06-01T10:00:00Z")).contains("90 days");
	}

	private String rule(String startsAt, String endsAt) {
		Api.Response response = book(ana, space, startsAt, endsAt);
		assertThat(response.status()).as(response.body()).isEqualTo(422);
		return response.json("$.detail");
	}

	@Test
	void malformedRequestsAreRejected() {
		assertThat(api.post("/api/bookings", ana, "{not json").status()).isEqualTo(400);
		assertThat(api.post("/api/bookings", ana, "{\"spaceId\": \"" + space + "\"}").status()).isEqualTo(400);
		assertThat(book(ana, "00000000-0000-0000-0000-000000000000", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")
			.status()).isEqualTo(404);
		assertThat(book(null, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").status()).isEqualTo(401);
	}

	@Test
	void aUserCanHoldAtMostFiveUpcomingBookings() {
		for (int day = 8; day <= 12; day++) {
			String date = "2030-01-%02d".formatted(day);
			assertThat(book(ana, space, date + "T09:00:00Z", date + "T10:00:00Z").status()).isEqualTo(201);
		}

		Api.Response sixth = book(ana, space, "2030-01-13T09:00:00Z", "2030-01-13T10:00:00Z");

		assertThat(sixth.status()).isEqualTo(422);
		assertThat(sixth.<String>json("$.detail")).contains("5 upcoming bookings");
	}

	@Test
	void listsUpcomingBookingsSoonestFirstAndEverythingOnRequest() {
		String later = book(ana, space, "2030-01-09T09:00:00Z", "2030-01-09T10:00:00Z").json("$.id");
		String sooner = book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z").json("$.id");
		String cancelled = book(ana, space, "2030-01-10T09:00:00Z", "2030-01-10T10:00:00Z").json("$.id");
		api.post("/api/bookings/" + cancelled + "/cancel", ana, null);
		book(ben, space, "2030-01-11T09:00:00Z", "2030-01-11T10:00:00Z");

		Api.Response upcoming = api.get("/api/bookings", ana);
		Api.Response all = api.get("/api/bookings?scope=all&size=2", ana);

		assertThat(upcoming.<List<String>>json("$.items[*].id")).containsExactly(sooner, later);
		assertThat(all.<List<String>>json("$.items[*].id")).containsExactly(cancelled, later);
		assertThat(all.<Integer>json("$.totalItems")).isEqualTo(3);
		assertThat(all.<Integer>json("$.totalPages")).isEqualTo(2);
		assertThat(api.get("/api/bookings?scope=yesterday", ana).status()).isEqualTo(422);
	}

	@Test
	void spacesThatStopTakingBookingsRefuseNewOnes() {
		assertThat(api.delete("/api/spaces/" + space, admin()).status()).isEqualTo(204);

		Api.Response response = book(ana, space, "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z");

		assertThat(response.status()).isEqualTo(422);
		assertThat(response.<String>json("$.detail")).contains("not taking bookings");
	}

}
