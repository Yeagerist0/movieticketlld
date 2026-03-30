package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Theatre {
    private final String id;
    private final String name;
    private final City   city;
    private final String address;
    // Screens added by admin; protected by synchronized methods
    private final List<Screen> screens = new ArrayList<>();

    private Theatre(String name, City city, String address) {
        this.id      = UUID.randomUUID().toString();
        this.name    = name;
        this.city    = city;
        this.address = address;
    }

    public static Theatre create(String name, City city, String address) {
        return new Theatre(name, city, address);
    }

    public synchronized void addScreen(Screen screen) {
        screens.add(screen);
    }

    public synchronized List<Screen> getScreens() {
        return Collections.unmodifiableList(new ArrayList<>(screens));
    }

    public String getId()      { return id; }
    public String getName()    { return name; }
    public City   getCity()    { return city; }
    public String getAddress() { return address; }

    @Override
    public String toString() {
        return "Theatre[" + name + " | " + city.getName() + " | " + screens.size() + " screen(s)]";
    }
}
