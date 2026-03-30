package services;

import models.Movie;
import models.Screen;
import models.Show;
import models.Theatre;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages movies and show-browsing APIs.
 *
 * showMovies(city) returns all movies currently screening in any theatre in that city.
 * addMovie is an admin operation.
 */
public class MovieService {

    // movieId -> Movie
    private final ConcurrentHashMap<String, Movie> movies = new ConcurrentHashMap<>();

    private final TheatreService theatreService;

    private MovieService(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    public static MovieService create(TheatreService theatreService) {
        return new MovieService(theatreService);
    }

    /** Admin: registers a new movie in the system. */
    public Movie addMovie(String title, String genre, int durationMinutes, String language) {
        Movie movie = Movie.create(title, genre, durationMinutes, language);
        movies.put(movie.getId(), movie);
        System.out.println("  [Admin] Movie added: " + movie);
        return movie;
    }

    /**
     * User API: returns distinct movies playing in any theatre in the given city.
     */
    public List<Movie> showMovies(String cityName) {
        List<Theatre> theatres = theatreService.showTheatres(cityName);
        Set<String>   seen     = new HashSet<>();
        List<Movie>   result   = new ArrayList<>();

        for (Theatre theatre : theatres) {
            for (Screen screen : theatre.getScreens()) {
                for (Show show : screen.getShows()) {
                    String movieId = show.getMovie().getId();
                    if (seen.add(movieId)) {
                        result.add(show.getMovie());
                    }
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Movie getMovie(String movieId) {
        return movies.get(movieId);
    }
}
