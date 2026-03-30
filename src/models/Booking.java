package models;

import enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Booking {
    private final String        id;
    private final User          user;
    private final Show          show;
    private final List<Seat>    seats;
    private final double        totalAmount;
    private       BookingStatus status;
    private final LocalDateTime bookedAt;
    private       Payment       payment;

    private Booking(User user, Show show, List<Seat> seats, double totalAmount) {
        this.id          = UUID.randomUUID().toString();
        this.user        = user;
        this.show        = show;
        this.seats       = List.copyOf(seats);
        this.totalAmount = totalAmount;
        this.status      = BookingStatus.PENDING;
        this.bookedAt    = LocalDateTime.now();
    }

    public static Booking create(User user, Show show, List<Seat> seats, double totalAmount) {
        return new Booking(user, show, seats, totalAmount);
    }

    public void setStatus(BookingStatus status) { this.status = status; }
    public void setPayment(Payment payment)     { this.payment = payment; }

    public String        getId()          { return id; }
    public User          getUser()        { return user; }
    public Show          getShow()        { return show; }
    public List<Seat>    getSeats()       { return seats; }
    public double        getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus()      { return status; }
    public LocalDateTime getBookedAt()    { return bookedAt; }
    public Payment       getPayment()     { return payment; }

    @Override
    public String toString() {
        return "Booking[" + id + " | " + show.getMovie().getTitle()
            + " | " + seats.size() + " seat(s) | ₹" + totalAmount
            + " | " + status + "]";
    }
}
