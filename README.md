# Movie Ticket Booking System — Low-Level Design

Java implementation of a BookMyShow-style booking system LLD, focused on concurrency-safe seat booking and pricing strategy rather than just the happy-path CRUD flow.

**Core flows.** Browse theatres and movies by city, book seats on a show, and cancel a booking for a refund, wired through `TheatreService`, `MovieService`, `ShowService`, `BookingService`, and `CancellationService`.

**Concurrency-safe seat locking.** Two users racing for the same seat is handled explicitly — the demo fires concurrent booking attempts from multiple threads (`ExecutorService` + `CountDownLatch`) at the same seat and verifies only one booking wins, via `SeatLockService`.

**Concurrent admin writes.** The demo also exercises two admins adding shows to the same screen at the same time, to check for race conditions in show creation.

**Pricing strategy.** Ticket price is computed through a swappable `PricingStrategy` — `StandardPricingStrategy` for flat pricing and `DynamicPricingStrategy` for weekend/evening multipliers.

## Structure

`models/`, `services/`, `strategies/`, `enums/`, `exceptions/`, and `Main.java` (a runnable demo covering bookings, cancellations, both concurrency scenarios, and dynamic pricing).

## Run

Compile with `javac` and run `Main`, or open the project in your IDE. It's a self-contained console demo with no external dependencies.
