package services;

import models.SeatLock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the 5-minute temporary seat holds.
 *
 * Key: "showId::seatId"
 *
 * A ScheduledExecutorService runs every minute to evict expired locks so that
 * abandoned booking attempts (user closed browser etc.) automatically free seats.
 */
public class SeatLockService {

    private static final int LOCK_TIMEOUT_MINUTES = 5;

    // ConcurrentHashMap for thread-safe individual key operations
    private final ConcurrentHashMap<String, SeatLock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService             sweeper;

    private SeatLockService() {
        sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "seat-lock-sweeper");
            t.setDaemon(true);
            return t;
        });
        // Sweep every minute for expired locks
        sweeper.scheduleAtFixedRate(this::sweepExpired, 1, 1, TimeUnit.MINUTES);
    }

    public static SeatLockService create() {
        return new SeatLockService();
    }

    private String key(String showId, String seatId) {
        return showId + "::" + seatId;
    }

    /**
     * Acquires a 5-min hold on a seat for a user.
     * Returns true if lock was acquired; false if seat is already locked by someone else.
     * Called AFTER the Show-level pessimistic lock confirms availability.
     */
    public boolean lockSeat(String showId, String seatId, String userId) {
        String key  = key(showId, seatId);
        SeatLock existing = locks.get(key);

        // Allow re-lock if previous lock expired or belongs to same user
        if (existing != null && !existing.isExpired() && !existing.getUserId().equals(userId)) {
            return false;
        }

        SeatLock lock = SeatLock.create(seatId, showId, userId,
                                        LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES));
        locks.put(key, lock);
        return true;
    }

    /** Releases locks for all seats after payment success or failure. */
    public void releaseLocks(String showId, List<String> seatIds) {
        for (String seatId : seatIds) locks.remove(key(showId, seatId));
    }

    /** True if the seat is currently locked by ANY user (and lock hasn't expired). */
    public boolean isLocked(String showId, String seatId) {
        SeatLock lock = locks.get(key(showId, seatId));
        if (lock == null) return false;
        if (lock.isExpired()) {
            locks.remove(key(showId, seatId));
            return false;
        }
        return true;
    }

    private void sweepExpired() {
        locks.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public void shutdown() {
        sweeper.shutdownNow();
    }
}
