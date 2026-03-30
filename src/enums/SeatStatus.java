package enums;

public enum SeatStatus {
    AVAILABLE,
    TEMPORARILY_HELD,  // 5-minute lock acquired, awaiting payment
    BOOKED
}
