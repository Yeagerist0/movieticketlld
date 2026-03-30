package services;

import models.Movie;
import models.Screen;
import models.Seat;
import models.Show;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin-facing show management service.
 *
 * addShow() — concurrent admin calls are safe:
 *   Screen.addShow() acquires a write lock internally, so two admins
 *   scheduling shows on the same screen at the same time won't corrupt the list.
 *   BookingService.registerShow() uses a ConcurrentHashMap.
 */
public class ShowService {

    private final ConcurrentHashMap<String, Show> showRegistry = new ConcurrentHashMap<>();
    private final BookingService bookingService;

    private ShowService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public static ShowService create(BookingService bookingService) {
        return new ShowService(bookingService);
    }

    /**
     * Admin: schedules a new show on a screen.
     * Uses the screen's seating layout as the show's seat inventory.
     * Write lock inside Screen.addShow() prevents concurrent admin corruption.
     */
    public Show addShow(Movie movie, Screen screen, LocalDateTime startTime) {
        List<Seat> seats = screen.getSeats();
        Show show = Show.create(movie, screen, startTime, seats);

        // Screen.addShow() is write-locked — safe for concurrent admin calls
        screen.addShow(show);

        // Register in BookingService so bookTickets() can resolve it
        bookingService.registerShow(show);
        showRegistry.put(show.getId(), show);

        System.out.println("  [Admin] Show scheduled: " + show);
        return show;
    }

    public Show getShow(String showId) {
        return showRegistry.get(showId);
    }
}
