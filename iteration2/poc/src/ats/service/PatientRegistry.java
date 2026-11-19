package ats.service;

import ats.domain.Patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-memory store of Patients (Pure Fabrication). Information Expert for finding Patients and
 * Creator of Patient. Patients are keyed by patientNumber and searchable by phone, because some
 * have no RAMQ number. The list is shared with CsvImportExport, so imported patients are found too.
 */
public class PatientRegistry {
    private final List<Patient> patients;

    public PatientRegistry() {
        this(new ArrayList<>());
    }

    /** @param shared the same mutable list given to CsvImportExport */
    public PatientRegistry(List<Patient> shared) {
        this.patients = Objects.requireNonNull(shared);
    }

    /** SO01: patientKey is a patient number or a phone number (punctuation in phone numbers is ignored). */
    public List<Patient> findByKey(String patientKey) {
        List<Patient> matches = new ArrayList<>();
        if (patientKey == null || patientKey.isBlank()) return matches;
        String key = patientKey.trim();
        String keyDigits = digits(key);
        for (Patient p : patients) {
            boolean numberMatch = String.valueOf(p.getPatientNumber()).equals(key);
            boolean phoneMatch = keyDigits.length() >= 7 && keyDigits.equals(digits(p.getPhone()));
            if (numberMatch || phoneMatch) matches.add(p);
        }
        return matches;
    }

    public List<Patient> findByPhone(String phone) {
        List<Patient> matches = new ArrayList<>();
        String d = digits(phone);
        for (Patient p : patients) {
            if (!d.isEmpty() && d.equals(digits(p.getPhone()))) matches.add(p);
        }
        return matches;
    }

    /** SO18: creates the Patient, or returns the existing one when the same person is already registered. */
    public Patient register(String firstName, String lastName, LocalDate dateOfBirth, String phone) {
        Patient existing = findDuplicate(firstName, lastName, dateOfBirth);
        if (existing != null) return existing;
        Patient p = new Patient(firstName, lastName, phone, dateOfBirth);
        patients.add(p);
        return p;
    }

    /** Same first name, last name and date of birth. Imported patients have no date of birth, so they never match here. */
    public Patient findDuplicate(String firstName, String lastName, LocalDate dateOfBirth) {
        if (firstName == null || lastName == null || dateOfBirth == null) return null;
        for (Patient p : patients) {
            if (p.getFirstName().equalsIgnoreCase(firstName.trim())
                    && p.getLastName().equalsIgnoreCase(lastName.trim())
                    && Objects.equals(p.getDateOfBirth(), dateOfBirth)) {
                return p;
            }
        }
        return null;
    }

    public List<Patient> getAll() {
        return Collections.unmodifiableList(patients);
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }
}
