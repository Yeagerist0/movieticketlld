package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A physical screen inside a Theatre.
 * Maintains a fixed seating layout and a list of Shows.
 *
 * Concurrency: ReentrantReadWriteLock on shows list.
 *   - Admin adding a show   → write lock
 *   - Users browsing shows  → read lock (multiple concurrent reads allowed)
 */
public class Screen {
    private final String      id;
    private final String      name;
    private final Theatre     theatre;
    private final List<Seat>  seats;   // fixed layout, set once at construction
    private final List<Show>  shows  = new ArrayList<>();

    private final ReentrantReadWriteLock showsLock = new ReentrantReadWriteLock();

    private Screen(String name, Theatre theatre, List<Seat> seats) {
        this.id      = UUID.randomUUID().toString();
        this.name    = name;
        this.theatre = theatre;
        this.seats   = Collections.unmodifiableList(new ArrayList<>(seats));
    }

    public static Screen create(String name, Theatre theatre, List<Seat> seats) {
        return new Screen(name, theatre, seats);
    }

    /** Admin operation — write-locked so no user can read a partial state. */
    public void addShow(Show show) {
        showsLock.writeLock().lock();
        try {
            shows.add(show);
        } finally {
            showsLock.writeLock().unlock();
        }
    }

    /** User operation — read-locked; concurrent reads are fine. */
    public List<Show> getShows() {
        showsLock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(shows));
        } finally {
            showsLock.readLock().unlock();
        }
    }

    public String    getId()      { return id; }
    public String    getName()    { return name; }
    public Theatre   getTheatre() { return theatre; }
    public List<Seat> getSeats()  { return seats; }

    @Override
    public String toString() {
        return "Screen[" + name + " | " + seats.size() + " seats]";
    }
}
