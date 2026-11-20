package ats.service;

import ats.domain.Appointment;
import ats.domain.ClinicLocation;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.io.CsvImportExport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that booking works on data imported from the handout's sample CSV files (Part A's importer)
 * because the menu shares the same lists. Run from iteration2/poc:
 *   java -cp out ats.service.CsvBookingIntegrationTest
 */
public class CsvBookingIntegrationTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        ProfessionalRegistry professionals = new ProfessionalRegistry();
        AvailabilityService availability = new AvailabilityService();
        List<Patient> patientList = new ArrayList<>();
        List<Appointment> appointmentList = new ArrayList<>();
        CsvImportExport csv = new CsvImportExport(professionals, availability,
                new ClinicLocation("Main Clinic", null, null), patientList, appointmentList);
        PatientRegistry patients = new PatientRegistry(patientList);
        AppointmentRegistry appointments = new AppointmentRegistry(appointmentList);
        BookingController controller = new BookingController(patients, appointments, new ActivityLog(), "test staff");

        csv.importAvailability(Path.of("data/professional-availability.csv"));
        csv.importAppointments(Path.of("data/booked-appointments.csv"));
        csv.importWaitlistPatients(Path.of("data/waitlist-patients.csv"));
        check("8 slots imported", availability.getAllSlots().size() == 8);
        check("4 open slots left after booked appointments", availability.searchAvailability(SearchCriteria.any()).size() == 4);

        List<Patient> fatima = controller.startBooking("438-555-0103");
        check("imported patient without RAMQ found by phone", fatima.size() == 1
                && fatima.get(0).getRamqNumber() == null && fatima.get(0).getLastName().equals("Benali"));

        List<Patient> maya = controller.startBooking("4385550203");
        check("waitlist patient found by phone and booking started", maya.size() == 1 && controller.getCurrentPatient() == maya.get(0));
        TimeSlot slot = availability.searchAvailability(SearchCriteria.any()).get(0);
        Appointment a = controller.bookTimeSlot(slot);
        check("booking an imported slot works", a.getSlot() == slot && !slot.isOpen());
        check("booking is visible to the CSV export list", appointmentList.contains(a));
        check("3 open slots left", availability.searchAvailability(SearchCriteria.any()).size() == 3);

        Path dir = Files.createTempDirectory("ats-test-");
        int exported = csv.exportAvailability(dir.resolve("export.csv"));
        String text = Files.readString(dir.resolve("export.csv"));
        check("export contains all 8 slots and the new booking", exported == 8 && text.contains("Maya") && text.contains("BOOKED"));

        // Emilie Lavoie was imported as booked 2026-11-02 09:00-09:30 with Dr. Tremblay.
        // A new doctor's slot at the same time must be refused for her (UC1 E5b, across professionals).
        ats.domain.HealthcareProfessional other = professionals.findOrCreate("Dr.", "Test", "Other", "Physician");
        TimeSlot clash = availability.createAvailability(other, new ClinicLocation("Laval", null, null),
                java.time.LocalDate.of(2026, 11, 2), java.time.LocalDate.of(2026, 11, 2), new DailyPattern(),
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(9, 30), 30).get(0);
        controller.startBooking("514-555-0101");
        boolean refused = false;
        try {
            controller.bookTimeSlot(clash);
        } catch (OverlappingAppointmentException e) {
            refused = e.getConflict().getPatient().getLastName().equals("Lavoie");
        }
        check("imported booking blocks an overlapping booking with another professional", refused && clash.isOpen());

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }
}
