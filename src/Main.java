import enums.SeatType;
import models.*;
import services.*;
import strategies.DynamicPricingStrategy;
import strategies.StandardPricingStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demo covering every required scenario:
 *  1.  showTheatres(city)
 *  2.  showMovies(city)
 *  3.  bookTickets(showId, seatIds) → MovieTicket
 *  4.  cancelBooking(bookingId)     → Refund
 *  5.  Concurrency: two users racing for the same seat (only one wins)
 *  6.  Concurrency: two admins adding shows simultaneously
 *  7.  Dynamic pricing (weekend evening multiplier)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("══════════ MOVIE TICKET BOOKING SYSTEM ══════════\n");

        // ── Wire up services ──────────────────────────────────────────────────
        SeatLockService     seatLockService     = SeatLockService.create();
        PaymentService      paymentService      = PaymentService.create();
        TheatreService      theatreService      = TheatreService.create();
        BookingService      bookingService      = BookingService.create(
                                                    seatLockService,
                                                    paymentService,
                                                    StandardPricingStrategy.create());
        ShowService         showService         = ShowService.create(bookingService);
        MovieService        movieService        = MovieService.create(theatreService);
        CancellationService cancellationService = CancellationService.create(bookingService, paymentService);

        // ── Admin: add movies ─────────────────────────────────────────────────
        System.out.println("─── Admin: Adding Movies ───");
        Movie interstellar = movieService.addMovie("Interstellar", "Sci-Fi",   169, "English");
        Movie animal       = movieService.addMovie("Animal",       "Thriller", 201, "Hindi");
        Movie oppenheimer  = movieService.addMovie("Oppenheimer",  "Drama",    180, "English");

        // ── Admin: add theatres ───────────────────────────────────────────────
        System.out.println("\n─── Admin: Adding Theatres ───");
        City delhi  = City.create("Delhi");
        City mumbai = City.create("Mumbai");

        Theatre pvr1     = theatreService.addTheatre("PVR Saket",     delhi,  "Saket District Centre");
        Theatre inox1    = theatreService.addTheatre("INOX R-City",   mumbai, "R-City Mall, Ghatkopar");
        Theatre cinepolis = theatreService.addTheatre("Cinepolis DT", delhi,  "DT City Centre");

        // ── Admin: add screens ────────────────────────────────────────────────
        System.out.println("\n─── Admin: Adding Screens ───");
        List<Seat> screen1Seats = buildSeats(5, 10); // 5 rows × 10 seats
        List<Seat> screen2Seats = buildSeats(4, 8);
        List<Seat> screen3Seats = buildSeats(3, 6);

        Screen screen1 = Screen.create("Audi 1", pvr1,      screen1Seats);
        Screen screen2 = Screen.create("Audi 2", inox1,     screen2Seats);
        Screen screen3 = Screen.create("Audi 1", cinepolis, screen3Seats);

        pvr1.addScreen(screen1);
        inox1.addScreen(screen2);
        cinepolis.addScreen(screen3);
        System.out.println("  Screen added: " + screen1);
        System.out.println("  Screen added: " + screen2);
        System.out.println("  Screen added: " + screen3);

        // ── Admin: add shows (including concurrent addition) ──────────────────
        System.out.println("\n─── Admin: Scheduling Shows (including concurrent) ───");

        // Saturday evening — dynamic pricing will apply 1.5× multiplier
        LocalDateTime satEvening = LocalDateTime.of(2026, 4, 4, 20, 0);
        LocalDateTime monMorning = LocalDateTime.of(2026, 4, 6, 10, 0);

        Show show1 = showService.addShow(interstellar, screen1, satEvening);
        Show show2 = showService.addShow(animal,       screen2, satEvening);
        Show show3 = showService.addShow(oppenheimer,  screen3, monMorning);

        // Two admins adding shows to screen1 simultaneously
        CountDownLatch adminLatch = new CountDownLatch(1);
        ExecutorService adminPool = Executors.newFixedThreadPool(2);
        adminPool.submit(() -> {
            try { adminLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            showService.addShow(animal, screen1, LocalDateTime.of(2026, 4, 4, 15, 0));
        });
        adminPool.submit(() -> {
            try { adminLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            showService.addShow(oppenheimer, screen1, LocalDateTime.of(2026, 4, 4, 23, 0));
        });
        adminLatch.countDown();
        adminPool.shutdown();
        Thread.sleep(200); // wait for both admin threads

        // ── API 1: showTheatres(city) ─────────────────────────────────────────
        System.out.println("\n─── showTheatres(\"Delhi\") ───");
        theatreService.showTheatres("Delhi").forEach(t -> System.out.println("  " + t));

        // ── API 2: showMovies(city) ───────────────────────────────────────────
        System.out.println("\n─── showMovies(\"Delhi\") ───");
        movieService.showMovies("Delhi").forEach(m -> System.out.println("  " + m));

        System.out.println("\n─── showMovies(\"Mumbai\") ───");
        movieService.showMovies("Mumbai").forEach(m -> System.out.println("  " + m));

        // ── API 3: bookTickets (normal) ───────────────────────────────────────
        System.out.println("\n─── Booking: User 1 books 2 seats on show1 (Standard Pricing) ───");
        User arjun  = User.create("Arjun Sharma", "arjun@example.com", "9876543210");
        User priya  = User.create("Priya Singh",  "priya@example.com", "9123456789");

        List<Seat> show1Seats = screen1Seats;
        String seatA1 = show1Seats.get(0).getId();
        String seatA2 = show1Seats.get(1).getId();
        MovieTicket ticket1 = bookingService.bookTickets(show1.getId(), List.of(seatA1, seatA2), arjun);
        System.out.println(ticket1);

        // ── Dynamic pricing demo ──────────────────────────────────────────────
        System.out.println("\n─── Dynamic Pricing: weekend evening show (1.5× multiplier) ───");
        BookingService dynamicBookingService = BookingService.create(
                seatLockService, paymentService, DynamicPricingStrategy.create());
        dynamicBookingService.registerShow(show2);
        List<Seat> show2Seats = screen2Seats;
        String seatB1 = show2Seats.get(0).getId(); // SILVER
        String seatB2 = show2Seats.get(5).getId(); // GOLD
        MovieTicket ticket2 = dynamicBookingService.bookTickets(show2.getId(), List.of(seatB1, seatB2), priya);
        System.out.println(ticket2);

        // ── API 4: cancelBooking + refund ─────────────────────────────────────
        System.out.println("\n─── Cancel: Arjun cancels booking ───");
        // Show is in the future (2026-04-04), so full refund applies
        cancellationService.cancelBooking(ticket1.getBooking().getId());

        // ── Concurrency: two users race for same seat ─────────────────────────
        System.out.println("\n─── Concurrency: User2 & User3 both try to book seat A3 on show3 ───");
        User user2 = User.create("Ravi Kumar",   "ravi@example.com", "9000000001");
        User user3 = User.create("Sneha Gupta",  "sneha@example.com","9000000002");
        String seatA3 = screen3Seats.get(0).getId();

        CountDownLatch ready  = new CountDownLatch(2);
        CountDownLatch start  = new CountDownLatch(1);
        int[]          wins   = {0, 0}; // [user2wins, user3wins]

        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            try {
                MovieTicket t = bookingService.bookTickets(show3.getId(), List.of(seatA3), user2);
                wins[0] = 1;
                System.out.println("  [Thread-User2] WON: " + t.getBooking().getId());
            } catch (Exception e) {
                System.out.println("  [Thread-User2] LOST: " + e.getMessage());
            }
        });

        pool.submit(() -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            try {
                MovieTicket t = bookingService.bookTickets(show3.getId(), List.of(seatA3), user3);
                wins[1] = 1;
                System.out.println("  [Thread-User3] WON: " + t.getBooking().getId());
            } catch (Exception e) {
                System.out.println("  [Thread-User3] LOST: " + e.getMessage());
            }
        });

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        System.out.println("  Result: exactly " + (wins[0] + wins[1]) + " of 2 users succeeded (expected: 1)");

        seatLockService.shutdown();
        System.out.println("\n══════════ DONE ══════════");
    }

    /**
     * Builds a seat grid: rows A,B,C... × 1..seatsPerRow
     *   First 40% of seats → SILVER
     *   Middle 40%         → GOLD
     *   Last 20%           → PLATINUM
     */
    private static List<Seat> buildSeats(int rows, int seatsPerRow) {
        List<Seat> seats  = new ArrayList<>();
        int        total  = rows * seatsPerRow;
        int        silver = (int) (total * 0.4);
        int        gold   = (int) (total * 0.4);
        int        idx    = 0;

        for (int r = 0; r < rows; r++) {
            String row = String.valueOf((char) ('A' + r));
            for (int s = 1; s <= seatsPerRow; s++) {
                SeatType type = idx < silver ? SeatType.SILVER
                             : idx < silver + gold ? SeatType.GOLD
                             : SeatType.PLATINUM;
                seats.add(Seat.create(row, s, type));
                idx++;
            }
        }
        return seats;
    }
}
