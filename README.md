# Booking API

A REST API for booking shared spaces (meeting rooms, desks, phone booths) in 15-minute slots, built with **Java 21 and Spring Boot 4**.

Its central promise: **two bookings for the same space can never overlap, even when requests arrive at the same instant.** That guarantee does not live in an `if` in the service layer, where two concurrent requests could both pass the check. It is a Postgres exclusion constraint, and a test fires 20 simultaneous requests at the same slot to prove exactly one wins.

## Run it

Requires Docker with Compose.

```bash
cp .env.example .env
docker compose up --build
```

Open the interactive documentation at **[http://localhost:8080/docs](http://localhost:8080/docs)**. Demo data is created on first start:

| Account | Email | Password |
|---|---|---|
| User | `demo@example.com` | `demo-password` |
| Admin | `admin@example.com` | `admin-demo-password` |

In Swagger UI, call `POST /api/auth/login`, copy the `accessToken`, press **Authorize** and paste it. Then try booking a time that is already taken.

## Endpoints

| Method | Path | Who | Does |
|---|---|---|---|
| `POST` | `/api/auth/register` | anyone | Create an account, returns a token |
| `POST` | `/api/auth/login` | anyone | Exchange email and password for a token |
| `GET` | `/api/me` | signed in | The current account |
| `GET` | `/api/spaces` | anyone | Spaces taking bookings |
| `GET` | `/api/spaces/{id}/availability?date=` | anyone | Free and booked time on a date, in the space's time zone |
| `POST` `PUT` `DELETE` | `/api/spaces[/{id}]` | admin | Manage spaces (delete stops new bookings, keeps history) |
| `POST` | `/api/bookings` | signed in | Book a space; `409` if any part of the time is taken |
| `GET` | `/api/bookings?scope=upcoming\|all` | signed in | Your bookings, paginated |
| `GET` | `/api/bookings/{id}` | owner or admin | One booking |
| `POST` | `/api/bookings/{id}/cancel` | owner or admin | Cancel before it starts |

Errors use the standard [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format, with a field-by-field `errors` map for validation failures:

```json
{
  "status": 409,
  "title": "Conflict",
  "detail": "This space is already booked for part of that time.",
  "instance": "/api/bookings"
}
```

## How double bookings are prevented

```sql
constraint bookings_no_overlap exclude using gist (
    space_id with =,
    tstzrange(starts_at, ends_at, '[)') with &&
) where (status = 'CONFIRMED')
```

Read it as: *no two confirmed rows may have the same space and overlapping time ranges.*

- **`[)` ranges** include the start and exclude the end, so a booking ending at 10:00 and another starting at 10:00 are both allowed.
- **`where (status = 'CONFIRMED')`** makes cancelled bookings invisible to the constraint, so cancelling frees the time immediately.
- **`btree_gist`** lets a single GiST index combine plain equality (`space_id`) with range overlap (`&&`).

The service does not check for overlaps first. It inserts, and if Postgres answers with SQLSTATE `23P01` (exclusion violation), it translates that into `409 Conflict`. A pre-check would add a query and still need the constraint for the race it cannot see.

`ConcurrentBookingIntegrationTest` proves it end to end: 20 users, 20 threads, a latch that releases every request at once, and the assertion that exactly one gets `201` and nineteen get `409`. A second test inserts rows with plain JDBC, bypassing the application entirely, and the database still refuses the overlap.

## Booking rules

Checked in [`BookingPolicy`](src/main/java/io/github/chipitaps/booking/booking/BookingPolicy.java), a pure class with no Spring or database dependencies, so every rule has a fast unit test:

- Start and end on a quarter hour, **in the space's local time**, so zones with half-hour offsets such as India still get sensible slots.
- Between 15 minutes and 4 hours, within the space's opening hours on a single day.
- In the future and at most 90 days ahead.
- At most 5 upcoming bookings per person. The user's row is locked (`SELECT … FOR UPDATE`) while counting, so two simultaneous requests cannot both slip under the limit.

Each space has an IANA time zone. Times are stored as UTC `timestamptz` and returned with the space's offset (`2030-01-08T09:00:00-05:00`), and opening hours are converted per date, so daylight saving changes are handled; there is a test for the March transition in New York.

## Security

- **Stateless JWT authentication** with Spring Security's OAuth2 resource server: HS256 tokens, 2-hour lifetime, issuer checked, roles carried in a `roles` claim and mapped to `ROLE_USER` / `ROLE_ADMIN`. Editing the role inside a token breaks the signature, and a test checks that a forged admin token is rejected.
- Passwords hashed with **bcrypt**. Login takes the same time whether or not the email exists, and the error message is identical in both cases.
- Five failed logins lock that email for 15 minutes (`429`).
- Admin endpoints use method security (`@PreAuthorize("hasRole('ADMIN')")`). Bookings that are not yours answer `404`, not `403`, so their existence is not revealed.
- The application refuses to start without a `JWT_SECRET` of at least 32 characters.

## Architecture

```
src/main/java/io/github/chipitaps/booking
├── auth       register, login, JWT issuing, login throttling
├── booking    booking rules, service, controller
├── space      spaces, opening hours, availability calculation
├── user       accounts and roles
├── common     problem details, time ranges, pagination
├── config     security, clock, OpenAPI
└── demo       optional demo data
```

| | |
|---|---|
| Runtime | Java 21, Spring Boot 4.1 (Spring MVC, Spring Security 7, Spring Data JPA, Hibernate 7) |
| Database | Postgres 17, Flyway migrations, `ddl-auto=validate` so Hibernate never changes the schema |
| API docs | springdoc-openapi, Swagger UI at `/docs` |
| Tests | JUnit 5, AssertJ, Testcontainers |
| Delivery | Multi-stage Docker build with layered jar, non-root user, health check; GitHub Actions |

Time is read from an injected `Clock` everywhere, including token expiry checks, so tests can move time forward to check expired tokens, started bookings and lock-out windows without sleeping.

## Tests

```bash
./mvnw verify
```

Needs Docker running; Testcontainers starts a real Postgres 17.

| Suite | Covers |
|---|---|
| `BookingPolicyTest`, `AvailabilityCalculatorTest`, `LoginAttemptsTest` | Pure rules: alignment, opening hours across time zones and DST, free-slot calculation, lock-out windows |
| `AuthIntegrationTest` | Registration, case-insensitive emails, login throttling, expired and forged tokens |
| `SpaceIntegrationTest` | Admin-only management, validation, availability in local time |
| `BookingIntegrationTest` | Conflicts, back-to-back bookings, cancellation, ownership, limits, pagination |
| `ConcurrentBookingIntegrationTest` | 20 simultaneous requests for one slot; the constraint without the application |

Integration tests talk to the application over real HTTP on a random port, so they exercise the security filter chain, JSON mapping and error handling exactly as a client would.

CI runs the full suite, then starts the Compose stack and books through the running API, including a double booking that must fail.

## Development without Docker Compose

```bash
./mvnw spring-boot:test-run
```

Starts the API on port 8080 against a throwaway Postgres container (still needs Docker). To use your own database set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` and `JWT_SECRET`, then `./mvnw spring-boot:run`.

## Roadmap

- [ ] Recurring bookings
- [ ] Refresh tokens and logout
- [ ] Capacity-aware booking for shared desks (several people per slot)
- [ ] Email confirmations through an outbox table
- [ ] Rate limiting shared across instances

## License

[MIT](LICENSE)
