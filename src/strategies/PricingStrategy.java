package strategies;

import models.Seat;
import models.Show;

import java.util.List;

/**
 * Strategy interface for computing total ticket price.
 * Implement to add new pricing algorithms (peak-hour, loyalty, etc.)
 * without touching BookingService.
 */
public interface PricingStrategy {
    double calculateTotal(Show show, List<Seat> seats);
}
