package ats.service;

import ats.domain.Appointment;
import ats.domain.Insurance;
import ats.domain.Patient;
import ats.domain.TimeSlot;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case controller for UC1 Request Appointment (GRASP Controller). Receives the system operations
 * SO01 startBooking, SO02 bookTimeSlot, SO03 cancelBooking and SO18 registerPatient (UC1 E1a) and
 * delegates. Search Availability (SO26) stays on AvailabilityService (Part B).
 */
public class BookingController {
    private final PatientRegistry patients;
    private final AppointmentRegistry appointments;
    private final ActivityLog log;
    private final String staffName;
    private Patient currentPatient;

    public BookingController(PatientRegistry patients, AppointmentRegistry appointments, ActivityLog log, String staffName) {
        this.patients = patients;
        this.appointments = appointments;
        this.log = log;
        this.staffName = staffName;
    }

    /** SO01 / CO1. Starts a booking only when exactly one Patient matches (E1a: none, E2a: several). */
    public List<Patient> startBooking(String patientKey) {
        List<Patient> matches = patients.findByKey(patientKey);
        if (matches.size() == 1) {
            setCurrentPatient(matches.get(0));
        } else {
            setCurrentPatient(null);
        }
        return matches;
    }

    /** SO02 / CO2. Books the TimeSlot for the current Patient; the slot is used up. */
    public Appointment bookTimeSlot(TimeSlot ts) {
        Patient p = currentPatient;
        if (p == null) {
            throw new IllegalStateException("No booking in progress. Find or register the patient first.");
        }
        if (ts == null || !ts.isOpen()) {
            throw new SlotUnavailableException("This time slot is no longer available.");
        }
        Appointment conflict = appointments.findOverlapping(p, ts);
        if (conflict != null) {
            throw new OverlappingAppointmentException(conflict);
        }
        Appointment a;
        try {
            a = appointments.book(p, ts);
        } catch (IllegalStateException e) {
            // Taken between isOpen() and the Appointment constructor: same outcome as E5a.
            throw new SlotUnavailableException("This time slot is no longer available.");
        }
        log.record(staffName, "appointment booked", p);
        setCurrentPatient(null);
        return a;
    }

    /** SO03 / CO3. Discards the unfinished booking; nothing was held, so nothing is released. */
    public void cancelBooking() {
        setCurrentPatient(null);
    }

    /** SO18 / CO18 (UC1 E1a). Insurance may be null (some patients have no RAMQ). */
    public Patient registerPatient(String firstName, String lastName, LocalDate dateOfBirth, String phone, Insurance insurance) {
        boolean isNew = patients.findDuplicate(firstName, lastName, dateOfBirth) == null;
        Patient p = patients.register(firstName, lastName, dateOfBirth, phone);
        if (isNew) {
            if (insurance != null) {
                p.addInsurance(insurance);
            }
            log.record(staffName, "patient registered", p);
        }
        setCurrentPatient(p);
        return p;
    }

    public Patient getCurrentPatient() { return currentPatient; }

    private void setCurrentPatient(Patient p) { this.currentPatient = p; }
}
