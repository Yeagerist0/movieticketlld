package services;

import models.City;
import models.Theatre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages theatres.
 *
 * showTheatres(city) is the primary user-facing API.
 * addTheatre / addScreen are admin operations.
 *
 * Thread safety: ConcurrentHashMap; individual Theatre.addScreen() is synchronized.
 */
public class TheatreService {

    // city name (lowercase) -> list of theatres
    private final ConcurrentHashMap<String, List<Theatre>> cityTheatres = new ConcurrentHashMap<>();

    private TheatreService() {}

    public static TheatreService create() {
        return new TheatreService();
    }

    /** Admin: registers a new theatre. */
    public Theatre addTheatre(String name, City city, String address) {
        Theatre theatre = Theatre.create(name, city, address);
        cityTheatres.computeIfAbsent(city.getName(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(theatre);
        System.out.println("  [Admin] Theatre added: " + theatre);
        return theatre;
    }

    /**
     * User API: returns all theatres in a city.
     */
    public List<Theatre> showTheatres(String cityName) {
        List<Theatre> result = cityTheatres.getOrDefault(cityName.toLowerCase(), Collections.emptyList());
        return Collections.unmodifiableList(new ArrayList<>(result));
    }
}
