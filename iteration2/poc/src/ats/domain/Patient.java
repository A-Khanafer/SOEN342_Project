package ats.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Patient {

    private static int counter = 1;

    private final int patientNumber;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final LocalDate dateOfBirth;   // may be null: the sample CSVs have no birth dates
    private final List<Insurance> insurances = new ArrayList<>();

    public Patient(String firstName, String lastName, String phone, LocalDate dateOfBirth) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }

        this.patientNumber = counter;
        counter++;
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
    }

    public void addInsurance(Insurance insurance) {
        if (insurance != null) {
            insurances.add(insurance);
        }
    }

    /** The patient's RAMQ number, or null if they have no RAMQ coverage. */
    public String getRamqNumber() {
        for (Insurance insurance : insurances) {
            if (insurance.getType() == InsuranceType.RAMQ) {
                return insurance.getNumber();
            }
        }
        return null;
    }

    public List<Insurance> getInsurances() {
        return Collections.unmodifiableList(insurances);
    }

    public int getPatientNumber() {
        return patientNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public String toString() {
        return "#" + patientNumber + " " + getFullName();
    }
}