package strategies;

import enums.SeatType;
import models.Seat;
import models.Show;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Demand + time-based pricing strategy.
 *
 * Multipliers applied on top of base rates:
 *   Weekend (Sat/Sun)                 → 1.3×
 *   Evening show (18:00 – 23:00)      → 1.2×
 *   Both weekend AND evening           → 1.5×
 *   Otherwise                          → 1.0×
 *
 * New algorithms can be added by creating another PricingStrategy implementation —
 * zero changes to BookingService required.
 */
public class DynamicPricingStrategy implements PricingStrategy {

    private static final double SILVER_BASE   = 150.0;
    private static final double GOLD_BASE     = 250.0;
    private static final double PLATINUM_BASE = 400.0;

    private DynamicPricingStrategy() {}

    public static DynamicPricingStrategy create() {
        return new DynamicPricingStrategy();
    }

    @Override
    public double calculateTotal(Show show, List<Seat> seats) {
        double multiplier = getMultiplier(show);
        double total = 0;
        for (Seat seat : seats) {
            total += baseRateFor(seat.getSeatType()) * multiplier;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private double getMultiplier(Show show) {
        DayOfWeek day  = show.getStartTime().getDayOfWeek();
        int       hour = show.getStartTime().getHour();

        boolean isWeekend  = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        boolean isEvening  = hour >= 18 && hour < 23;

        if (isWeekend && isEvening) return 1.5;
        if (isWeekend)              return 1.3;
        if (isEvening)              return 1.2;
        return 1.0;
    }

    private double baseRateFor(SeatType type) {
        switch (type) {
            case SILVER:   return SILVER_BASE;
            case GOLD:     return GOLD_BASE;
            case PLATINUM: return PLATINUM_BASE;
            default: throw new IllegalArgumentException("Unknown seat type: " + type);
        }
    }
}
