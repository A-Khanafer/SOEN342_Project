package ats;

import ats.domain.*;
import ats.service.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class App {
    public static void main(String[] args) {
        AvailabilityService availability = new AvailabilityService();
        ClinicLocation clinic = new ClinicLocation("Main Clinic", null, null);

        MedSpecialty cardiology = new MedSpecialty("Cardiology");
        MedSpecialty dermatology = new MedSpecialty("Dermatology");

        HealthcareProfessional bouchard = new HealthcareProfessional("Dr.", "Marc", "Bouchard", null, "Physician");
        bouchard.addSpecialty(cardiology);
        HealthcareProfessional haddad = new HealthcareProfessional("Dr.", "Nadia", "Haddad", null, "Physician");
        haddad.addSpecialty(dermatology);

        // ---------- Make Availability ----------
        // Spec example: every Monday in November 2026, 9:00-12:00, 30-minute slots
        List<TimeSlot> created = availability.createAvailability(bouchard, clinic,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                new WeeklyPattern(Set.of(DayOfWeek.MONDAY)),
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
        System.out.println("Created " + created.size() + " slots (expected 30)");
        System.out.println("First: " + created.get(0));
        System.out.println("Last:  " + created.get(created.size() - 1));

        // Same request again: must be refused, nothing added
        try {
            availability.createAvailability(bouchard, clinic,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                    new WeeklyPattern(Set.of(DayOfWeek.MONDAY)),
                    LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
        } catch (IllegalStateException e) {
            System.out.println("Second request refused (expected)");
        }
        System.out.println("Total slots still " + availability.getAllSlots().size() + " (expected 30)");

        // Monthly on the 31st, Nov 1 - Dec 31: only Dec 31 exists, 9:00-10:45 -> 3 slots (15 min dropped)
        List<TimeSlot> monthly = availability.createAvailability(bouchard, clinic,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                new MonthlyPattern(31), LocalTime.of(9, 0), LocalTime.of(10, 45), 30);
        System.out.println("Monthly on the 31st created " + monthly.size() + " slots (expected 3)");

        // Haddad: Nov 3, 13:00-14:00 -> 2 slots
        availability.createAvailability(haddad, clinic,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3),
                new DailyPattern(), LocalTime.of(13, 0), LocalTime.of(14, 0), 30);

        // ---------- Search Availability ----------
        System.out.println();
        System.out.println("All open slots: " + availability.searchAvailability(SearchCriteria.any()).size()
                + " (expected 35)");

        List<TimeSlot> derm = availability.searchAvailability(new SearchCriteria(null, dermatology, null, null));
        System.out.println("Dermatology: " + derm.size() + " (expected 2)");
        derm.forEach(s -> System.out.println("  " + s));

        List<TimeSlot> week1 = availability.searchAvailability(
                new SearchCriteria(null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)));
        System.out.println("Nov 1-7: " + week1.size() + " (expected 8: 6 Bouchard on Nov 2 + 2 Haddad on Nov 3)");

        // Booking a slot removes it from search results
        new Appointment(new Patient("Fatima", "Benali", "438-555-0103", null), derm.get(0));
        System.out.println("Dermatology after booking one: "
                + availability.searchAvailability(new SearchCriteria(null, dermatology, null, null)).size()
                + " (expected 1)");
    }
}