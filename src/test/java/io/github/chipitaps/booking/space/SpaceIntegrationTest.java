package io.github.chipitaps.booking.space;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chipitaps.booking.support.Api;
import io.github.chipitaps.booking.support.IntegrationTest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpaceIntegrationTest extends IntegrationTest {

	@Test
	void onlyAdminsCanAddSpaces() {
		String user = register("Ana");
		String admin = admin();

		assertThat(api.post("/api/spaces", null, spaceJson("Loft", "UTC", "08:00", "20:00")).status()).isEqualTo(401);
		assertThat(api.post("/api/spaces", user, spaceJson("Loft", "UTC", "08:00", "20:00")).status()).isEqualTo(403);

		Api.Response created = api.post("/api/spaces", admin, spaceJson("Loft", "America/Bogota", "07:00", "21:00"));
		assertThat(created.status()).isEqualTo(201);
		assertThat(created.location()).hasValue("/api/spaces/" + created.json("$.id"));
		assertThat(created.<String>json("$.timeZone")).isEqualTo("America/Bogota");
	}

	@Test
	void anyoneCanBrowseActiveSpaces() {
		String admin = admin();
		createSpace(admin, "Zen Room", "UTC", "08:00", "20:00");
		String closed = createSpace(admin, "Attic", "UTC", "08:00", "20:00");
		assertThat(api.delete("/api/spaces/" + closed, admin).status()).isEqualTo(204);

		Api.Response list = api.get("/api/spaces", null);

		assertThat(list.status()).isEqualTo(200);
		assertThat(list.<List<String>>json("$[*].name")).containsExactly("Zen Room");
		assertThat(api.get("/api/spaces/" + closed, null).<Boolean>json("$.active")).isFalse();
	}

	@Test
	void namesAreUniqueRegardlessOfCase() {
		String admin = admin();
		createSpace(admin, "Board Room", "UTC", "08:00", "20:00");

		Api.Response duplicate = api.post("/api/spaces", admin, spaceJson("board room", "UTC", "08:00", "20:00"));

		assertThat(duplicate.status()).isEqualTo(409);
	}

	@Test
	void rejectsUnknownTimeZonesAndImpossibleHours() {
		String admin = admin();

		Api.Response zone = api.post("/api/spaces", admin, spaceJson("A", "Mars/Olympus", "08:00", "20:00"));
		Api.Response hours = api.post("/api/spaces", admin, spaceJson("B", "UTC", "20:00", "08:00"));
		Api.Response quarter = api.post("/api/spaces", admin, spaceJson("C", "UTC", "08:10", "20:00"));
		Api.Response missing = api.post("/api/spaces", admin, "{\"name\": \"D\"}");

		assertThat(zone.status()).isEqualTo(422);
		assertThat(zone.<String>json("$.detail")).contains("not a known time zone");
		assertThat(hours.status()).isEqualTo(422);
		assertThat(quarter.status()).isEqualTo(422);
		assertThat(missing.status()).isEqualTo(400);
		assertThat(missing.<String>json("$.errors.capacity")).isEqualTo("Say how many people fit.");
	}

	@Test
	void adminsCanChangeASpace() {
		String admin = admin();
		String id = createSpace(admin, "Studio", "UTC", "08:00", "20:00");

		Api.Response updated = api.put("/api/spaces/" + id, admin, spaceJson("Studio B", "UTC", "09:00", "17:00"));

		assertThat(updated.status()).isEqualTo(200);
		assertThat(updated.<String>json("$.name")).isEqualTo("Studio B");
		assertThat(updated.<String>json("$.opensAt")).startsWith("09:00");
	}

	@Test
	void availabilityShowsWhatIsFreeAndBookedInTheSpacesTimeZone() {
		String admin = admin();
		String id = createSpace(admin, "Focus", "America/Bogota", "08:00", "18:00");
		String user = register("Ana");
		assertThat(book(user, id, "2030-01-08T10:00:00-05:00", "2030-01-08T11:30:00-05:00").status()).isEqualTo(201);

		Api.Response day = api.get("/api/spaces/" + id + "/availability?date=2030-01-08", null);

		assertThat(day.status()).isEqualTo(200);
		assertThat(day.<String>json("$.timeZone")).isEqualTo("America/Bogota");
		assertThat(times(day, "$.free[*].startsAt")).containsExactly(at("2030-01-08T08:00:00-05:00"),
				at("2030-01-08T11:30:00-05:00"));
		assertThat(times(day, "$.free[*].endsAt")).containsExactly(at("2030-01-08T10:00:00-05:00"),
				at("2030-01-08T18:00:00-05:00"));
		assertThat(times(day, "$.booked[*].startsAt")).containsExactly(at("2030-01-08T10:00:00-05:00"));
		assertThat(day.<List<String>>json("$.free[*].startsAt")).allMatch((value) -> value.endsWith("-05:00"));
	}

	@Test
	void availabilityRejectsBadDatesAndUnknownSpaces() {
		String id = createSpace("Focus");

		assertThat(api.get("/api/spaces/" + id + "/availability?date=tomorrow", null).status()).isEqualTo(400);
		assertThat(api.get("/api/spaces/" + id + "/availability", null).status()).isEqualTo(400);
		assertThat(api.get("/api/spaces/00000000-0000-0000-0000-000000000000/availability?date=2030-01-08", null)
			.status()).isEqualTo(404);
		assertThat(api.get("/api/spaces/not-a-uuid", null).status()).isEqualTo(400);
	}

	private static List<OffsetDateTime> times(Api.Response response, String path) {
		return response.<List<String>>json(path).stream().map(OffsetDateTime::parse).toList();
	}

	private static OffsetDateTime at(String value) {
		return OffsetDateTime.parse(value);
	}

}
