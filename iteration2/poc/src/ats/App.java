package ats;

import ats.domain.*;
import ats.service.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class App {
    public static void main(String[] args) {
        ProfessionalRegistry professionals = new ProfessionalRegistry();
        AvailabilityService availability = new AvailabilityService();
        ClinicLocation clinic = new ClinicLocation("Main Clinic", null, null);

        // Fix #1: two CSV rows for the same doctor give back the SAME object
        HealthcareProfessional row1 = professionals.findOrCreate("Dr.", "Amélie", "Tremblay", "Physician");
        HealthcareProfessional row2 = professionals.findOrCreate("Dr.", "amélie ", "TREMBLAY", "Physician");
        System.out.println("Same object for both rows: " + (row1 == row2) + " (expected true)");
        System.out.println("Professionals registered: " + professionals.getAll().size() + " (expected 1)");
        row1.addSpecialty(new MedSpecialty("Family Medicine"));

        availability.createAvailability(row1, clinic,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2), new DailyPattern(),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        try {
            availability.createAvailability(row2, clinic,
                    LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2), new DailyPattern(),
                    LocalTime.of(9, 30), LocalTime.of(10, 30), 30);
            System.out.println("Overlap NOT detected (bug)");
        } catch (IllegalStateException e) {
            System.out.println("Overlap via second CSV row refused (expected)");
        }

        List<TimeSlot> hers = availability.searchAvailability(new SearchCriteria(row2, null, null, null));
        System.out.println("Search by professional finds " + hers.size() + " slots (expected 2)");

        // Fix #3: slot status only changes through Appointment
        Patient emilie = new Patient("Émilie", "Lavoie", "514-555-0101", null);
        Appointment appt = new Appointment(emilie, hers.get(0));
        System.out.println("Slot open after booking? " + hers.get(0).isOpen() + " (expected false)");
        appt.cancel();
        System.out.println("Slot open after cancel? " + hers.get(0).isOpen() + " (expected true)");
        // hers.get(0).release();   // uncomment to check: this line no longer compiles
    }
}