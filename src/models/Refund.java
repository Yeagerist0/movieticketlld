package models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Refund {
    private final String        id;
    private final String        bookingId;
    private final double        refundAmount;
    private final LocalDateTime refundedAt;

    private Refund(String bookingId, double refundAmount) {
        this.id           = UUID.randomUUID().toString();
        this.bookingId    = bookingId;
        this.refundAmount = refundAmount;
        this.refundedAt   = LocalDateTime.now();
    }

    public static Refund create(String bookingId, double refundAmount) {
        return new Refund(bookingId, refundAmount);
    }

    public String        getId()           { return id; }
    public String        getBookingId()    { return bookingId; }
    public double        getRefundAmount() { return refundAmount; }
    public LocalDateTime getRefundedAt()  { return refundedAt; }

    @Override
    public String toString() {
        return "Refund[bookingId=" + bookingId + " | ₹" + refundAmount + " | at " + refundedAt + "]";
    }
}
