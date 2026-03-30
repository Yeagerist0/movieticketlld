package models;

import enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    private final String        id;
    private final String        bookingId;
    private final double        amount;
    private       PaymentStatus status;
    private final LocalDateTime paidAt;

    private Payment(String bookingId, double amount) {
        this.id        = UUID.randomUUID().toString();
        this.bookingId = bookingId;
        this.amount    = amount;
        this.status    = PaymentStatus.SUCCESS;
        this.paidAt    = LocalDateTime.now();
    }

    public static Payment create(String bookingId, double amount) {
        return new Payment(bookingId, amount);
    }

    public void setStatus(PaymentStatus status) { this.status = status; }

    public String        getId()        { return id; }
    public String        getBookingId() { return bookingId; }
    public double        getAmount()    { return amount; }
    public PaymentStatus getStatus()    { return status; }
    public LocalDateTime getPaidAt()    { return paidAt; }

    @Override
    public String toString() {
        return "Payment[₹" + amount + " | " + status + " | " + paidAt + "]";
    }
}
