package models;

import java.time.LocalDateTime;

/**
 * Represents a 5-minute temporary hold placed on a seat for a specific show.
 * SeatLockService manages these; expired locks are auto-released.
 */
public class SeatLock {
    private final String        seatId;
    private final String        showId;
    private final String        userId;
    private final LocalDateTime expiresAt;

    private SeatLock(String seatId, String showId, String userId, LocalDateTime expiresAt) {
        this.seatId    = seatId;
        this.showId    = showId;
        this.userId    = userId;
        this.expiresAt = expiresAt;
    }

    public static SeatLock create(String seatId, String showId, String userId, LocalDateTime expiresAt) {
        return new SeatLock(seatId, showId, userId, expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String        getSeatId()    { return seatId; }
    public String        getShowId()    { return showId; }
    public String        getUserId()    { return userId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    @Override
    public String toString() {
        return "SeatLock[seat=" + seatId + " show=" + showId
            + " user=" + userId + " expires=" + expiresAt + "]";
    }
}
