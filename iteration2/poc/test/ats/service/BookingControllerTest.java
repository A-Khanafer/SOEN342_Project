package ats.service;

import ats.domain.Appointment;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.Insurance;
import ats.domain.MedSpecialty;
import ats.domain.InsuranceType;
import ats.domain.Patient;
import ats.domain.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Plain-Java tests for Part C (no JUnit needed). Run:
 *   javac -d out $(find src test -name "*.java") && java -cp out ats.service.BookingControllerTest
 */
public class BookingControllerTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        bookingUsesUpTheSlot();
        bookingSameSlotTwiceIsRefused();
        overlappingAppointmentIsRefused();
        unknownPatientStartsNoBooking();
        sharedPhoneNeedsPatientNumber();
        patientWithoutRamqCanBeRegisteredAndFound();
        duplicateRegistrationReturnsExistingPatient();
        cancelBookingRecordsNothing();
        cancelledAppointmentFreesTheSlot();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // --- fixtures -------------------------------------------------------------------------------

    private static PatientRegistry patients;
    private static AppointmentRegistry appointments;
    private static ActivityLog log;
    private static AvailabilityService availability;
    private static BookingController controller;
    private static HealthcareProfessional drX;

    private static void setUp() {
        patients = new PatientRegistry();
        appointments = new AppointmentRegistry();
        log = new ActivityLog();
        availability = new AvailabilityService();
        controller = new BookingController(patients, appointments, log, "test staff");
        ProfessionalRegistry pros = new ProfessionalRegistry();
        drX = pros.findOrCreate("Dr.", "Xavier", "Roy", "Physician");
        drX.addSpecialty(new MedSpecialty("Cardiology"));
        // November 2026, every Monday 9:00 to 12:00 in 30 minute slots (handout example).
        availability.createAvailability(drX, new ClinicLocation("Montreal Downtown", null, null),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                new WeeklyPattern(EnumSet.of(DayOfWeek.MONDAY)), LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
    }

    private static List<TimeSlot> open() {
        return availability.searchAvailability(new SearchCriteria(drX, null, null, null));
    }

    // --- tests ----------------------------------------------------------------------------------

    private static void bookingUsesUpTheSlot() {
        setUp();
        Patient p = controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        controller.startBooking(String.valueOf(p.getPatientNumber()));
        int before = open().size();
        TimeSlot slot = open().get(0);
        Appointment a = controller.bookTimeSlot(slot);
        check("appointment is for the patient", a.getPatient() == p);
        check("slot is no longer open", !slot.isOpen());
        check("search shows one slot fewer", open().size() == before - 1);
        check("appointment stored", appointments.findByPatient(p).size() == 1);
        check("booking is logged", log.getAll().stream().anyMatch(r -> r.getAction().equals("appointment booked")));
        check("booking ends after success", controller.getCurrentPatient() == null);
    }

    private static void bookingSameSlotTwiceIsRefused() {
        setUp();
        Patient p1 = controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        TimeSlot slot = open().get(0);
        // (registerPatient starts the booking for the new patient, as in UC1 E1a)
        controller.bookTimeSlot(slot);
        controller.registerPatient("Ben", "Tran", LocalDate.of(1985, 1, 9), "514-555-0202", null);
        boolean refused = false;
        try {
            controller.bookTimeSlot(slot);
        } catch (SlotUnavailableException e) {
            refused = true;
        }
        check("second booking of the same slot is refused (E5a)", refused);
        check("only one appointment exists", appointments.getAll().size() == 1 && appointments.getAll().get(0).getPatient() == p1);
    }

    private static void overlappingAppointmentIsRefused() {
        setUp();
        Patient p = controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        TimeSlot first = open().get(0);
        controller.bookTimeSlot(first);
        // A second professional with a slot at the same time on the same day.
        HealthcareProfessional drY = new ProfessionalRegistry().findOrCreate("Dr.", "Yara", "Diab", "Physician");
        TimeSlot same = availability.createAvailability(drY, new ClinicLocation("Laval", null, null), first.getDate(), first.getDate(),
                new WeeklyPattern(EnumSet.allOf(DayOfWeek.class)), first.getStartTime(), first.getEndTime(), 30).get(0);
        controller.startBooking(String.valueOf(p.getPatientNumber()));
        boolean refused = false;
        try {
            controller.bookTimeSlot(same);
        } catch (OverlappingAppointmentException e) {
            refused = e.getConflict().getSlot() == first;
        }
        check("overlapping appointment refused with the conflict (E5b)", refused);
        check("overlapping slot stays open", same.isOpen());
    }

    private static void unknownPatientStartsNoBooking() {
        setUp();
        List<Patient> m = controller.startBooking("999-999-9999");
        check("no match returns empty list (E1a)", m.isEmpty());
        boolean refused = false;
        try {
            controller.bookTimeSlot(open().get(0));
        } catch (IllegalStateException e) {
            refused = true;
        }
        check("cannot book without a patient", refused);
    }

    private static void sharedPhoneNeedsPatientNumber() {
        setUp();
        Patient mom = controller.registerPatient("Rita", "Haddad", LocalDate.of(1970, 3, 3), "(514) 555-0303", null);
        Patient kid = controller.registerPatient("Sami", "Haddad", LocalDate.of(2015, 8, 8), "514 555 0303", null);
        List<Patient> m = controller.startBooking("5145550303");
        check("shared phone returns both patients (E2a)", m.size() == 2 && controller.getCurrentPatient() == null);
        m = controller.startBooking(String.valueOf(kid.getPatientNumber()));
        check("patient number selects one patient", m.size() == 1 && controller.getCurrentPatient() == kid && mom != kid);
    }

    private static void patientWithoutRamqCanBeRegisteredAndFound() {
        setUp();
        Patient noRamq = controller.registerPatient("Lea", "Martin", LocalDate.of(2000, 2, 2), "438-555-0404", null);
        Patient withRamq = controller.registerPatient("Omar", "Said", LocalDate.of(1995, 6, 6), "438-555-0505",
                new Insurance(InsuranceType.RAMQ, "SAIO95060612", "RAMQ"));
        check("patient numbers are different", noRamq.getPatientNumber() != withRamq.getPatientNumber());
        check("patient without RAMQ found by phone", patients.findByPhone("438-555-0404").contains(noRamq));
        check("patient with RAMQ found by phone", patients.findByKey("4385550505").contains(withRamq));
        check("registration logged twice", log.getAll().size() == 2);
    }

    private static void duplicateRegistrationReturnsExistingPatient() {
        setUp();
        Patient a = controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        Patient b = controller.registerPatient("ana", "LOPEZ", LocalDate.of(1990, 5, 2), "514-555-9999", null);
        check("duplicate returns the existing patient", a == b && patients.getAll().size() == 1);
    }

    private static void cancelBookingRecordsNothing() {
        setUp();
        Patient p = controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        int openBefore = open().size();
        int logBefore = log.getAll().size();
        controller.startBooking(String.valueOf(p.getPatientNumber()));
        controller.cancelBooking();
        check("cancel ends the booking (CO3)", controller.getCurrentPatient() == null);
        check("cancel records nothing", appointments.getAll().isEmpty() && log.getAll().size() == logBefore && open().size() == openBefore);
    }

    private static void cancelledAppointmentFreesTheSlot() {
        setUp();
        controller.registerPatient("Ana", "Lopez", LocalDate.of(1990, 5, 2), "514-555-0101", null);
        TimeSlot slot = open().get(0);
        Appointment a = controller.bookTimeSlot(slot);
        a.cancel();
        check("cancelled appointment frees the slot", slot.isOpen());
        check("cancelled appointment no longer blocks an overlap", appointments.findOverlapping(a.getPatient(), slot) == null);
        controller.registerPatient("Ben", "Tran", LocalDate.of(1985, 1, 9), "514-555-0202", null);
        check("freed slot can be booked again", controller.bookTimeSlot(slot).getSlot() == slot);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
