package ats.domain;

import java.time.LocalDateTime;

public class Appointment {

    private final Patient patient;
    private final TimeSlot slot;
    private AppointmentStatus status = AppointmentStatus.BOOKED;
    private LocalDateTime arrivalTime;   // null until the patient arrives

    /** Creating an appointment books its slot; it fails if the slot is already taken. */
    public Appointment(Patient patient, TimeSlot slot) {
        if (patient == null || slot == null) {
            throw new IllegalArgumentException("Patient and time slot are required");
        }
        slot.book();
        this.patient = patient;
        this.slot = slot;
    }

    public void registerArrival() {
        if (status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only a booked appointment can be checked in");
        }
        status = AppointmentStatus.ARRIVED;
        arrivalTime = LocalDateTime.now();
    }

    public void cancel() {
        
        if (status != AppointmentStatus.BOOKED) {
        
            throw new IllegalStateException("Only a booked appointment can be cancelled");
        }
    
        status = AppointmentStatus.CANCELLED;
        slot.release();
    
    }

    public Patient getPatient() {
        return patient;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return patient + " | " + slot + " | " + status;
    }
}