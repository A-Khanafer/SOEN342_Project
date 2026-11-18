package ats;

import ats.domain.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class App {
    public static void main(String[] args) {
        ClinicLocation clinic = new ClinicLocation("Main Clinic", null, null);

        HealthcareProfessional tremblay =
                new HealthcareProfessional("Dr.", "Amélie", "Tremblay", null, "Physician");
        tremblay.addSpecialty(new MedSpecialty("Family Medicine"));
        tremblay.addSpecialty(new MedSpecialty("family medicine"));   // duplicate, ignored
        System.out.println(tremblay + " specialties: " + tremblay.getSpecialties());

        LocalDate nov2 = LocalDate.of(2026, 11, 2);
        TimeSlot a = new TimeSlot(tremblay, clinic, nov2, LocalTime.of(9, 0), LocalTime.of(9, 30));
        TimeSlot b = new TimeSlot(tremblay, clinic, nov2, LocalTime.of(9, 30), LocalTime.of(10, 0));
        TimeSlot c = new TimeSlot(tremblay, clinic, nov2, LocalTime.of(9, 15), LocalTime.of(9, 45));
        System.out.println("a overlaps b (touching): " + a.overlaps(b));
        System.out.println("a overlaps c: " + c.overlaps(a));

        Patient emilie = new Patient("Émilie", "Lavoie", "514-555-0101", null);
        emilie.addInsurance(new Insurance(InsuranceType.RAMQ, "LAVE99010101", null));
        Patient olivier = new Patient("Olivier", "Côté", "514-555-0102", null);
        System.out.println(emilie + " RAMQ " + emilie.getRamqNumber());
        System.out.println(olivier);

        Appointment appt = new Appointment(emilie, a);
        System.out.println(appt);

        try {
            new Appointment(olivier, a);   // slot already taken
        } catch (IllegalStateException e) {
            System.out.println("Double booking refused: " + e.getMessage());
        }

        appt.cancel();
        System.out.println("After cancel: " + appt.getStatus() + ", slot open again: " + a.isOpen());
    }
}