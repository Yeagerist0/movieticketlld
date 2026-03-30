package models;

import java.util.UUID;

public class Movie {
    private final String id;
    private final String title;
    private final String genre;
    private final int    durationMinutes;
    private final String language;

    private Movie(String title, String genre, int durationMinutes, String language) {
        this.id              = UUID.randomUUID().toString();
        this.title           = title;
        this.genre           = genre;
        this.durationMinutes = durationMinutes;
        this.language        = language;
    }

    public static Movie create(String title, String genre, int durationMinutes, String language) {
        return new Movie(title, genre, durationMinutes, language);
    }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getGenre()           { return genre; }
    public int    getDurationMinutes() { return durationMinutes; }
    public String getLanguage()        { return language; }

    @Override
    public String toString() {
        return "Movie[" + title + " | " + language + " | " + durationMinutes + "min]";
    }
}
