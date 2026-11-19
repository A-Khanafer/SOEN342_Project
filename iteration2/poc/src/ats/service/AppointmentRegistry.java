package ats.service;

import ats.domain.Appointment;
import ats.domain.AppointmentStatus;
import ats.domain.Patient;
import ats.domain.TimeSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-memory store of Appointments (Pure Fabrication, same class as in the UC2 design).
 * Creator of Appointment (it records Appointments) and Information Expert for the patient
 * overlap check (it knows every Appointment). The list is shared with CsvImportExport.
 */
public class AppointmentRegistry {
    private final List<Appointment> appointments;

    public AppointmentRegistry() {
        this(new ArrayList<>());
    }

    /** @param shared the same mutable list given to CsvImportExport */
    public AppointmentRegistry(List<Appointment> shared) {
        this.appointments = Objects.requireNonNull(shared);
    }

    /** SO02: creating the Appointment books the TimeSlot (the constructor throws if it is already taken). */
    public Appointment book(Patient patient, TimeSlot slot) {
        Appointment a = new Appointment(patient, slot);
        appointments.add(a);
        return a;
    }

    /** UC1 E5b: a non-cancelled Appointment of this Patient whose TimeSlot overlaps in time, or null. */
    public Appointment findOverlapping(Patient patient, TimeSlot slot) {
        for (Appointment x : appointments) {
            if (x.getPatient() == patient && x.getStatus() != AppointmentStatus.CANCELLED
                    && timesOverlap(x.getSlot(), slot)) {
                return x;
            }
        }
        return null;
    }

    public List<Appointment> findByPatient(Patient patient) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getPatient() == patient) result.add(a);
        }
        return result;
    }

    public List<Appointment> getAll() {
        return Collections.unmodifiableList(appointments);
    }

    /**
     * Same date and intersecting [start, end) intervals. TimeSlot.overlaps only compares slots of the
     * same professional, but a patient cannot be in two places at once with different professionals.
     */
    private static boolean timesOverlap(TimeSlot a, TimeSlot b) {
        return a.getDate().equals(b.getDate())
                && a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
    }
}
