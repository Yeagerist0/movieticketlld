package models;

import enums.SeatType;

import java.util.UUID;

public class Seat {
    private final String   id;
    private final String   rowLabel;   // A, B, C ...
    private final int      seatNumber;
    private final SeatType seatType;

    private Seat(String rowLabel, int seatNumber, SeatType seatType) {
        this.id         = UUID.randomUUID().toString();
        this.rowLabel   = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType   = seatType;
    }

    public static Seat create(String rowLabel, int seatNumber, SeatType seatType) {
        return new Seat(rowLabel, seatNumber, seatType);
    }

    public String   getId()         { return id; }
    public String   getRowLabel()   { return rowLabel; }
    public int      getSeatNumber() { return seatNumber; }
    public SeatType getSeatType()   { return seatType; }

    @Override
    public String toString() {
        return rowLabel + seatNumber + "[" + seatType + "]";
    }
}
