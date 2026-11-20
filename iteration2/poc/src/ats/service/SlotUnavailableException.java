package ats.service;

/** UC1 E5a: the TimeSlot was booked after it was presented. */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) { super(message); }
}
