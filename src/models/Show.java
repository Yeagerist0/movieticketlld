package models;

import enums.SeatStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A specific screening of a Movie on a Screen at a scheduled time.
 *
 * Concurrency model — pessimistic locking:
 *   - showLock (ReentrantLock): held during multi-seat check-and-hold to make
 *     the "are all these seats free? → hold them all" operation atomic.
 *     This prevents two users from both seeing seat S1 as AVAILABLE and both
 *     proceeding to book it.
 *   - seatStatuses (ConcurrentHashMap): individual seat status is read without
 *     the lock for display purposes (seat map view); mutations always happen
 *     inside showLock.
 */
public class Show {
    private final String        id;
    private final Movie         movie;
    private final Screen        screen;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    // seatId -> SeatStatus; ConcurrentHashMap for safe lock-free reads
    private final ConcurrentHashMap<String, SeatStatus> seatStatuses;

    // Held during any multi-seat booking attempt
    private final ReentrantLock showLock = new ReentrantLock(true); // fair lock

    private Show(Movie movie, Screen screen, LocalDateTime startTime, List<Seat> seats) {
        this.id        = UUID.randomUUID().toString();
        this.movie     = movie;
        this.screen    = screen;
        this.startTime = startTime;
        this.endTime   = startTime.plusMinutes(movie.getDurationMinutes());

        this.seatStatuses = new ConcurrentHashMap<>();
        for (Seat seat : seats) {
            seatStatuses.put(seat.getId(), SeatStatus.AVAILABLE);
        }
    }

    public static Show create(Movie movie, Screen screen, LocalDateTime startTime, List<Seat> seats) {
        return new Show(movie, screen, startTime, seats);
    }

    // ── Lock-free seat status read (for seat map display) ─────────────────────

    public SeatStatus getSeatStatus(String seatId) {
        return seatStatuses.getOrDefault(seatId, SeatStatus.BOOKED);
    }

    public Map<String, SeatStatus> getAllSeatStatuses() {
        return Collections.unmodifiableMap(new HashMap<>(seatStatuses));
    }

    // ── Atomic multi-seat operations (called inside showLock) ─────────────────

    /**
     * Tries to temporarily hold all given seats.
     * All-or-nothing: if even one seat is unavailable, no seat is touched.
     * Caller MUST hold showLock before calling.
     */
    public void holdSeats(List<String> seatIds) {
        if (!showLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("showLock must be held before calling holdSeats");
        }
        for (String seatId : seatIds) {
            SeatStatus status = seatStatuses.get(seatId);
            if (status != SeatStatus.AVAILABLE) {
                throw new exceptions.SeatNotAvailableException(
                    "Seat " + seatId + " is not available (status: " + status + ")");
            }
        }
        for (String seatId : seatIds) {
            seatStatuses.put(seatId, SeatStatus.TEMPORARILY_HELD);
        }
    }

    /**
     * Confirms held seats to BOOKED.
     * Caller MUST hold showLock.
     */
    public void confirmSeats(List<String> seatIds) {
        for (String seatId : seatIds) seatStatuses.put(seatId, SeatStatus.BOOKED);
    }

    /**
     * Releases seats back to AVAILABLE (payment failure or cancellation).
     * Safe to call without showLock for single-seat release after booking.
     */
    public void releaseSeats(List<String> seatIds) {
        for (String seatId : seatIds) seatStatuses.put(seatId, SeatStatus.AVAILABLE);
    }

    public ReentrantLock getShowLock() { return showLock; }

    public String        getId()        { return id; }
    public Movie         getMovie()     { return movie; }
    public Screen        getScreen()    { return screen; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }

    @Override
    public String toString() {
        return "Show[" + movie.getTitle() + " | " + screen.getName()
            + " | " + startTime + "]";
    }
}
