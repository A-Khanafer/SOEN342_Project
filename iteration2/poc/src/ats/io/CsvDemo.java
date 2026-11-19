package ats.io;

import ats.domain.Appointment;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.service.AvailabilityService;
import ats.service.ProfessionalRegistry;
import ats.service.SearchCriteria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Console demonstration only; no assertions, test runner or menu replacement. */
public final class CsvDemo {
    private CsvDemo() { }

    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("Usage: java -cp out ats.io.CsvDemo [data-directory] [new-export-file]");
            return;
        }
        try {
            Path data = Path.of(args.length > 0 ? args[0] : "data").toAbsolutePath().normalize();
            Path output = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize()
                    : Files.createTempDirectory("ats-csv-demo-").resolve("availability.csv");
            ProfessionalRegistry professionals = new ProfessionalRegistry();
            AvailabilityService availability = new AvailabilityService();
            ClinicLocation clinic = new ClinicLocation("Main Clinic, \"Centre\"", null, null);
            List<Patient> patients = new ArrayList<>();
            List<Appointment> appointments = new ArrayList<>();
            CsvImportExport csv = new CsvImportExport(professionals, availability, clinic, patients, appointments);

            System.out.println("SOEN342 Part A — CSV console demonstration");
            System.out.println("Source directory: " + data);
            report("Professional availability CSV", csv.importAvailability(data.resolve("professional-availability.csv")));
            System.out.println("Professionals registered: " + professionals.getAll().size());
            List<HealthcareProfessional> sorted = new ArrayList<>(professionals.getAll());
            sorted.sort(Comparator.comparing(HealthcareProfessional::getFullName));
            for (HealthcareProfessional professional : sorted) {
                System.out.println("  " + professional + " | " + professional.getSpecialties());
                for (TimeSlot slot : availability.getAllSlots()) {
                    if (slot.getProfessional() == professional) System.out.println("    " + slot);
                }
            }

            report("Booked appointments CSV", csv.importAppointments(data.resolve("booked-appointments.csv")));
            System.out.println("Booked appointments and occupied slots:");
            for (Appointment appointment : appointments) System.out.println("  " + appointment);
            System.out.println("Available slots after importing appointments: "
                    + availability.searchAvailability(SearchCriteria.any()).size());
            for (TimeSlot slot : availability.searchAvailability(SearchCriteria.any())) System.out.println("  " + slot);
            report("Re-import booked appointments (duplicates skipped)",
                    csv.importAppointments(data.resolve("booked-appointments.csv")));

            report("Waitlist patient information CSV", csv.importWaitlistPatients(data.resolve("waitlist-patients.csv")));
            for (WaitlistPatientInfo info : csv.getWaitlistPatientInfo()) System.out.println("  " + info);
            System.out.println("Patients retained: " + patients.size());
            System.out.println("Waitlist information needs staff completion: service, criteria and priority; referral verification.");

            int exported = csv.exportAvailability(output);
            System.out.println("Exported CSV: " + output + " | records=" + exported);
            CsvImportExport restored = new CsvImportExport(new ProfessionalRegistry(), new AvailabilityService(),
                    clinic, new ArrayList<>(), new ArrayList<>());
            report("Restore exported availability into fresh memory", restored.importAvailability(output));
            System.out.println("Restored occupied appointments: " + restored.getAppointments().size());

            try {
                csv.importAvailability(data.resolve("missing-file.csv"));
            } catch (IOException e) {
                System.out.println("Missing-file message: " + e.getMessage());
            }
            try {
                csv.importAppointments(data.resolve("professional-availability.csv"));
            } catch (IOException e) {
                System.out.println("Wrong-header message: " + e.getMessage());
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("CSV demonstration could not complete: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void report(String label, ImportResult result) {
        System.out.println(label + ": " + result);
        for (String error : result.getErrors()) System.out.println("  " + error);
    }
}
