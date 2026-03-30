package services;

import enums.BookingStatus;
import exceptions.InvalidCancellationException;
import models.Booking;
import models.Refund;
import models.Show;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * Handles booking cancellation and refund computation.
 *
 * Refund policy (based on how far in advance the cancellation is made):
 *   > 24 hours before show  → 100% refund
 *   12–24 hours before show → 50% refund
 *   < 12 hours before show  → No refund
 */
public class CancellationService {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    private CancellationService(BookingService bookingService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    public static CancellationService create(BookingService bookingService, PaymentService paymentService) {
        return new CancellationService(bookingService, paymentService);
    }

    /**
     * Cancels a confirmed booking and processes the refund.
     *
     * @param bookingId The booking to cancel
     * @return          Refund details
     */
    public Refund cancelBooking(String bookingId) {
        Booking booking = bookingService.getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidCancellationException(
                "Cannot cancel booking in state: " + booking.getStatus());
        }

        Show show = booking.getShow();

        // Release seats back to AVAILABLE
        show.getShowLock().lock();
        try {
            show.releaseSeats(
                booking.getSeats().stream().map(s -> s.getId()).collect(Collectors.toList())
            );
        } finally {
            show.getShowLock().unlock();
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Compute refund
        double refundAmount = computeRefund(booking, show);
        Refund refund       = paymentService.processRefund(bookingId, refundAmount);

        System.out.println("  [Cancel] Booking " + bookingId + " cancelled.");
        System.out.println("  [Cancel] " + refund);
        return refund;
    }

    private double computeRefund(Booking booking, Show show) {
        long hoursUntilShow = ChronoUnit.HOURS.between(LocalDateTime.now(), show.getStartTime());

        if (hoursUntilShow > 24)  return booking.getTotalAmount();           // 100%
        if (hoursUntilShow >= 12) return booking.getTotalAmount() * 0.50;    // 50%
        return 0.0;                                                           // no refund
    }
}
