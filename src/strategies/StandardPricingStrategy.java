package strategies;

import enums.SeatType;
import models.Seat;
import models.Show;

import java.util.List;

/**
 * Base rates per seat type.
 *   Silver   → ₹150
 *   Gold     → ₹250
 *   Platinum → ₹400
 */
public class StandardPricingStrategy implements PricingStrategy {

    private static final double SILVER_RATE   = 150.0;
    private static final double GOLD_RATE     = 250.0;
    private static final double PLATINUM_RATE = 400.0;

    private StandardPricingStrategy() {}

    public static StandardPricingStrategy create() {
        return new StandardPricingStrategy();
    }

    @Override
    public double calculateTotal(Show show, List<Seat> seats) {
        double total = 0;
        for (Seat seat : seats) {
            total += rateFor(seat.getSeatType());
        }
        return total;
    }

    private double rateFor(SeatType type) {
        switch (type) {
            case SILVER:   return SILVER_RATE;
            case GOLD:     return GOLD_RATE;
            case PLATINUM: return PLATINUM_RATE;
            default: throw new IllegalArgumentException("Unknown seat type: " + type);
        }
    }
}
