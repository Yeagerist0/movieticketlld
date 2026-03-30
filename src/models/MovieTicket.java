package models;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Immutable invoice returned by bookTickets().
 * One MovieTicket per booking, covering all seats in that booking.
 */
public class MovieTicket {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final Booking    booking;
    private final List<Seat> seats;

    private MovieTicket(Booking booking) {
        this.booking = booking;
        this.seats   = booking.getSeats();
    }

    public static MovieTicket create(Booking booking) {
        return new MovieTicket(booking);
    }

    public Booking    getBooking() { return booking; }
    public List<Seat> getSeats()   { return seats; }

    @Override
    public String toString() {
        Show show = booking.getShow();
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════ MOVIE TICKET ══════════════╗\n");
        sb.append(String.format("  Booking ID : %s%n", booking.getId()));
        sb.append(String.format("  Customer   : %s%n", booking.getUser().getName()));
        sb.append(String.format("  Movie      : %s%n", show.getMovie().getTitle()));
        sb.append(String.format("  Theatre    : %s%n", show.getScreen().getTheatre().getName()));
        sb.append(String.format("  Screen     : %s%n", show.getScreen().getName()));
        sb.append(String.format("  Show Time  : %s%n", show.getStartTime().format(FMT)));
        sb.append("  Seats      : ");
        seats.forEach(s -> sb.append(s).append("  "));
        sb.append(String.format("%n  Amount     : ₹%.2f%n", booking.getTotalAmount()));
        sb.append(String.format("  Status     : %s%n", booking.getStatus()));
        sb.append("╚══════════════════════════════════════════╝");
        return sb.toString();
    }
}
