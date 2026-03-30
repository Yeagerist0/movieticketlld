package services;

import enums.BookingStatus;
import exceptions.SeatNotAvailableException;
import exceptions.ShowNotFoundException;
import models.*;
import strategies.PricingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core booking service.
 *
 * ─── Concurrency strategy (pessimistic locking) ───────────────────────────
 *
 *  1. Acquire Show.showLock (ReentrantLock) — guarantees that no two threads
 *     can execute the check-and-hold block simultaneously for the same show.
 *
 *  2. Inside the lock:
 *       a. Verify all requested seats are AVAILABLE in Show.seatStatuses
 *       b. If yes → flip them to TEMPORARILY_HELD (atomic from other threads)
 *       c. Register 5-min holds in SeatLockService
 *
 *  3. Release Show.showLock immediately after holding seats.
 *     Payment processing happens OUTSIDE the lock (no need to hold DB/show
 *     lock while waiting for a network call to a payment gateway).
 *
 *  4. On payment success  → confirmSeats (TEMPORARILY_HELD → BOOKED)
 *     On payment failure  → releaseSeats (TEMPORARILY_HELD → AVAILABLE)
 *
 * This ensures only ONE booking can grab a given seat across all concurrent
 * requests while keeping the lock window as small as possible.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class BookingService {

    // bookingId -> Booking
    private final ConcurrentHashMap<String, Booking> bookings = new ConcurrentHashMap<>();
    // showId -> Show
    private final ConcurrentHashMap<String, Show>    shows    = new ConcurrentHashMap<>();

    private final SeatLockService  seatLockService;
    private final PaymentService   paymentService;
    private final PricingStrategy  pricingStrategy;

    private BookingService(SeatLockService seatLockService,
                           PaymentService paymentService,
                           PricingStrategy pricingStrategy) {
        this.seatLockService = seatLockService;
        this.paymentService  = paymentService;
        this.pricingStrategy = pricingStrategy;
    }

    public static BookingService create(SeatLockService seatLockService,
                                        PaymentService paymentService,
                                        PricingStrategy pricingStrategy) {
        return new BookingService(seatLockService, paymentService, pricingStrategy);
    }

    public void registerShow(Show show) {
        shows.put(show.getId(), show);
    }

    // ── Main API ─────────────────────────────────────────────────────────────

    /**
     * Books tickets for a list of seat IDs on a specific show.
     *
     * @param showId  The show to book
     * @param seatIds List of seatIds the user wants
     * @param user    The user making the booking
     * @return        MovieTicket (confirmed invoice)
     */
    public MovieTicket bookTickets(String showId, List<String> seatIds, User user) {
        Show show = shows.get(showId);
        if (show == null) throw new ShowNotFoundException("Show not found: " + showId);

        // Resolve seat objects from the screen's layout
        List<Seat> requestedSeats = resolveSeats(show, seatIds);

        Booking booking = null;

        // ── CRITICAL SECTION — hold all requested seats atomically ────────────
        show.getShowLock().lock();
        try {
            // 1. All-or-nothing check + hold inside the show lock
            show.holdSeats(seatIds);

            // 2. Register 5-minute locks in SeatLockService
            for (String seatId : seatIds) {
                seatLockService.lockSeat(showId, seatId, user.getId());
            }

            // 3. Calculate price and create booking (PENDING)
            double total = pricingStrategy.calculateTotal(show, requestedSeats);
            booking = Booking.create(user, show, requestedSeats, total);
            bookings.put(booking.getId(), booking);

        } finally {
            show.getShowLock().unlock();
        }
        // ── END CRITICAL SECTION ──────────────────────────────────────────────

        // Payment is intentionally OUTSIDE the show lock
        try {
            Payment payment = paymentService.processPayment(booking.getId(), booking.getTotalAmount());
            booking.setPayment(payment);
            booking.setStatus(BookingStatus.CONFIRMED);

            // Confirm seats in the show
            show.getShowLock().lock();
            try {
                show.confirmSeats(seatIds);
            } finally {
                show.getShowLock().unlock();
            }

            // Release SeatLockService entries — seats are BOOKED now
            seatLockService.releaseLocks(showId, seatIds);

            System.out.println("  [Booking] Confirmed: " + booking.getId());
            return MovieTicket.create(booking);

        } catch (Exception e) {
            // Payment failed — roll back
            show.releaseSeats(seatIds);
            seatLockService.releaseLocks(showId, seatIds);
            booking.setStatus(BookingStatus.CANCELLED);
            throw new RuntimeException("Payment failed, seats released: " + e.getMessage(), e);
        }
    }

    public Booking getBooking(String bookingId) {
        Booking b = bookings.get(bookingId);
        if (b == null) throw new exceptions.BookingNotFoundException("Booking not found: " + bookingId);
        return b;
    }

    public Map<String, Booking> getAllBookings() { return bookings; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Seat> resolveSeats(Show show, List<String> seatIds) {
        Map<String, enums.SeatStatus> statuses = show.getAllSeatStatuses();
        List<Seat> result = new ArrayList<>();
        for (Seat seat : show.getScreen().getSeats()) {
            if (seatIds.contains(seat.getId())) {
                result.add(seat);
            }
        }
        if (result.size() != seatIds.size()) {
            throw new SeatNotAvailableException("One or more seatIds not found in this show's screen.");
        }
        return result;
    }
}
