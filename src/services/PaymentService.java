package services;

import enums.PaymentStatus;
import models.Payment;
import models.Refund;

/**
 * Simulates payment gateway calls.
 * In production this would call Razorpay / Stripe etc.
 */
public class PaymentService {

    private PaymentService() {}

    public static PaymentService create() {
        return new PaymentService();
    }

    /**
     * Charges the user. Returns a Payment with SUCCESS status.
     * In production: throw PaymentFailedException on gateway error.
     */
    public Payment processPayment(String bookingId, double amount) {
        System.out.println("  [Payment] Processing ₹" + amount + " for booking " + bookingId);
        Payment payment = Payment.create(bookingId, amount);
        payment.setStatus(PaymentStatus.SUCCESS);
        System.out.println("  [Payment] SUCCESS — id: " + payment.getId());
        return payment;
    }

    /**
     * Issues a refund for a cancelled booking.
     * Refund amount respects cancellation policy (computed externally).
     */
    public Refund processRefund(String bookingId, double refundAmount) {
        System.out.println("  [Refund] Processing ₹" + refundAmount + " for booking " + bookingId);
        Refund refund = Refund.create(bookingId, refundAmount);
        System.out.println("  [Refund] SUCCESS — " + refund);
        return refund;
    }
}
