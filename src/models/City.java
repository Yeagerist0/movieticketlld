package models;

public class City {
    private final String name;

    private City(String name) {
        this.name = name.toLowerCase();
    }

    public static City create(String name) {
        return new City(name);
    }

    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        return name.equals(((City) o).name);
    }

    @Override
    public int hashCode() { return name.hashCode(); }

    @Override
    public String toString() { return "City[" + name + "]"; }
}
