package ats.ui;

import ats.domain.ActivityRecord;
import ats.domain.Appointment;
import ats.domain.AppointmentStatus;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.Insurance;
import ats.domain.InsuranceType;
import ats.domain.MedSpecialty;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.io.CsvImportExport;
import ats.io.ImportResult;
import ats.io.WaitlistPatientInfo;
import ats.service.ActivityLog;
import ats.service.AppointmentRegistry;
import ats.service.AvailabilityService;
import ats.service.BookingController;
import ats.service.DailyPattern;
import ats.service.MonthlyPattern;
import ats.service.OverlappingAppointmentException;
import ats.service.PatientRegistry;
import ats.service.ProfessionalRegistry;
import ats.service.RecurrencePattern;
import ats.service.SearchCriteria;
import ats.service.SlotUnavailableException;
import ats.service.WeeklyPattern;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 * Console menu that runs the whole PoC: Make Availability (SO25), Search Availability (SO26),
 * Request Appointment / booking (SO01-SO03, SO18), cancel (SO17), Import CSV (SO31), Export CSV (SO32).
 * The menu is UI only: every system operation is delegated to a controller or service.
 * It owns the shared application state, so the CSV importer and the booking code see the same objects.
 */
public class ConsoleMenu {
    private final Scanner in;
    private final String staffName;
    private final ProfessionalRegistry professionals = new ProfessionalRegistry();
    private final AvailabilityService availability = new AvailabilityService();
    private final ClinicLocation mainClinic = new ClinicLocation("Main Clinic", null, null);
    private final List<Patient> patientList = new ArrayList<>();
    private final List<Appointment> appointmentList = new ArrayList<>();
    private final PatientRegistry patients = new PatientRegistry(patientList);
    private final AppointmentRegistry appointments = new AppointmentRegistry(appointmentList);
    private final ActivityLog log = new ActivityLog();
    private final BookingController booking;
    private final CsvImportExport csv;

    public ConsoleMenu(Scanner in, String staffName) {
        this.in = in;
        this.staffName = staffName;
        this.booking = new BookingController(patients, appointments, log, staffName);
        this.csv = new CsvImportExport(professionals, availability, mainClinic, patientList, appointmentList);
    }

    public static void main(String[] args) {
        new ConsoleMenu(new Scanner(System.in), "front desk").run();
    }

    public void run() {
        println("Appointment Tracking System (SOEN 342 PoC). The system starts empty: import the CSV files (7) or make availability (1).");
        while (true) {
            println("");
            println("=== Main menu ===");
            println(" 1. Make availability");
            println(" 2. Search availability");
            println(" 3. Book an appointment (patient at the desk or on the phone)");
            println(" 4. Register a patient");
            println(" 5. Find a patient and see their appointments");
            println(" 6. Cancel an appointment");
            println(" 7. Import from CSV");
            println(" 8. Export availability to CSV");
            println(" 9. Show imported waiting list information");
            println("10. Show activity log");
            println(" 0. Exit");
            String choice = ask("Choose");
            if (choice == null) return; // end of input
            try {
                switch (choice) {
                    case "1" -> makeAvailability();
                    case "2" -> searchAvailability();
                    case "3" -> bookAppointment();
                    case "4" -> registerPatient();
                    case "5" -> findPatient();
                    case "6" -> cancelAppointment();
                    case "7" -> importCsv();
                    case "8" -> exportCsv();
                    case "9" -> showWaitlistInfo();
                    case "10" -> showLog();
                    case "0" -> { println("Goodbye."); return; }
                    default -> println("Unknown option \"" + choice + "\". Type a number from the menu.");
                }
            } catch (EndOfInput e) {
                return;
            } catch (RuntimeException e) {
                println("Error: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ 1. Make availability (SO25, Part B)

    private void makeAvailability() {
        HealthcareProfessional prof = chooseProfessional(false);
        String locationName = ask("Clinic location (empty = Main Clinic)");
        if (locationName == null) throw new EndOfInput();
        ClinicLocation location = findLocation(locationName);
        LocalDate from = askDate("Start date (YYYY-MM-DD)", false);
        LocalDate to = askDate("End date (YYYY-MM-DD)", false);
        println("Repetition: 1 = every day, 2 = day(s) of the week, 3 = monthly (same day of the month)");
        RecurrencePattern pattern = null;
        while (pattern == null) {
            switch (askRequired("Pattern")) {
                case "1" -> pattern = new DailyPattern();
                case "2" -> pattern = new WeeklyPattern(askDays());
                case "3" -> pattern = new MonthlyPattern(askInt("Day of the month (1-31)", 1, 31));
                default -> println("Type 1, 2 or 3.");
            }
        }
        LocalTime start = askTime("Start time (HH:MM)");
        LocalTime end = askTime("End time (HH:MM)");
        int minutes = askInt("Slot length in minutes (for example 30, 45 or 60)", 5, 720);
        List<TimeSlot> created = availability.createAvailability(prof, location, from, to, pattern, start, end, minutes);
        println(created.size() + " time slot(s) created for " + prof + " (" + pattern + ").");
    }

    // ------------------------------------------------------------------ 2. Search availability (SO26, included in UC1)

    private List<TimeSlot> searchAvailability() {
        println("Leave a field empty to mean \"any\".");
        HealthcareProfessional prof = chooseProfessional(true);
        MedSpecialty specialty = prof == null ? chooseSpecialty() : null;
        LocalDate from = askDate("From date (YYYY-MM-DD)", true);
        LocalDate to = askDate("To date (YYYY-MM-DD)", true);
        if (from != null && to != null && to.isBefore(from)) {
            println("The \"to\" date is before the \"from\" date; searching from " + to + " to " + from + ".");
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        List<TimeSlot> open = availability.searchAvailability(new SearchCriteria(prof, specialty, from, to));
        if (open.isEmpty()) {
            println("No open time slots match.");
        } else {
            println(open.size() + " open time slot(s):");
            for (int i = 0; i < open.size(); i++) {
                println(String.format(" %3d. %s", i + 1, describe(open.get(i))));
            }
        }
        return open;
    }

    // ------------------------------------------------------------------ 3. Book an appointment (UC1, SO01-SO03)

    private void bookAppointment() {
        // M1-M2: identify the patient (SO01), or register them (E1a, SO18). E2a: shared phone.
        Patient patient = null;
        while (patient == null) {
            String key = askRequired("Patient phone number or patient number (\"new\" to register, \"cancel\" to stop)");
            if (key.equalsIgnoreCase("cancel")) {
                booking.cancelBooking();
                println("Booking cancelled. Nothing recorded.");
                return;
            }
            if (key.equalsIgnoreCase("new")) {
                patient = registerPatient();
                break;
            }
            List<Patient> matches = booking.startBooking(key);
            if (matches.size() == 1) {
                patient = matches.get(0);
            } else if (matches.isEmpty()) {
                println("No patient found. Type \"new\" to register them, or try again.");
            } else {
                println("Several patients share this phone number:");
                for (Patient p : matches) println("  " + describe(p));
                println("Enter the patient number of the right one.");
            }
        }
        println("Booking for " + describe(patient));

        // M3-M8: search (SO26) then book (SO02); E3a, E5a, E5b loop back to M3. "cancel" = SO03.
        while (true) {
            List<TimeSlot> open = searchAvailability();
            if (open.isEmpty()) {
                String again = ask("Search again? (y = yes, anything else cancels the booking)");
                if (again != null && again.equalsIgnoreCase("y")) continue;
                booking.cancelBooking();
                println("Booking cancelled. No appointment recorded (the patient can be placed on the waiting list, W1).");
                return;
            }
            String pick = askRequired("Number of the slot to book, \"s\" to search again, or \"cancel\"");
            if (pick.equalsIgnoreCase("cancel")) {
                booking.cancelBooking();
                println("Booking cancelled. No appointment recorded.");
                return;
            }
            if (pick.equalsIgnoreCase("s")) continue;
            int index;
            try {
                index = Integer.parseInt(pick.trim()) - 1;
            } catch (NumberFormatException e) {
                println("Not a number.");
                continue;
            }
            if (index < 0 || index >= open.size()) {
                println("No slot with that number.");
                continue;
            }
            try {
                Appointment a = booking.bookTimeSlot(open.get(index));
                println("Booked: " + describe(a));
                return;
            } catch (SlotUnavailableException e) {
                println(e.getMessage() + " Search again.");
            } catch (OverlappingAppointmentException e) {
                println(e.getMessage() + " Conflict: " + describe(e.getConflict()));
            }
        }
    }

    // ------------------------------------------------------------------ 4. Register a patient (SO18)

    private Patient registerPatient() {
        String first = askRequired("First name");
        String last = askRequired("Last name");
        LocalDate dob = askDate("Date of birth (YYYY-MM-DD)", false);
        String phone = askRequired("Phone");
        Insurance insurance = null;
        println("Insurance: 1 = RAMQ, 2 = private, empty = none (some patients have no RAMQ)");
        while (true) {
            String type = ask("Insurance");
            if (type == null) throw new EndOfInput();
            if (type.isBlank()) break;
            if (type.equals("1")) {
                insurance = new Insurance(InsuranceType.RAMQ, askRequired("RAMQ number"), "RAMQ");
                break;
            }
            if (type.equals("2")) {
                String provider = askRequired("Provider (for example Manulife)");
                String number = ask("Policy number (empty if unknown)");
                insurance = new Insurance(InsuranceType.PRIVATE, number == null || number.isBlank() ? null : number, provider);
                break;
            }
            println("Type 1, 2, or leave it empty.");
        }
        boolean existed = patients.findDuplicate(first, last, dob) != null;
        Patient p = booking.registerPatient(first, last, dob, phone, insurance);
        println((existed ? "Already registered: " : "Registered: ") + describe(p));
        return p;
    }

    // ------------------------------------------------------------------ 5. Find a patient

    private Patient findPatient() {
        String key = askRequired("Phone number or patient number");
        List<Patient> matches = patients.findByKey(key);
        if (matches.isEmpty()) {
            println("No patient found.");
            return null;
        }
        for (Patient p : matches) {
            println(describe(p));
            List<Appointment> list = appointments.findByPatient(p);
            if (list.isEmpty()) println("   no appointments");
            for (Appointment a : list) println("   " + describe(a));
        }
        if (matches.size() > 1) println("Several patients share this phone number. Use the patient number to pick one.");
        return matches.size() == 1 ? matches.get(0) : null;
    }

    // ------------------------------------------------------------------ 6. Cancel an appointment (SO17, frees the slot)

    private void cancelAppointment() {
        Patient p = findPatient();
        if (p == null) return;
        List<Appointment> booked = new ArrayList<>();
        for (Appointment a : appointments.findByPatient(p)) {
            if (a.getStatus() == AppointmentStatus.BOOKED) booked.add(a);
        }
        if (booked.isEmpty()) {
            println("No booked appointments to cancel.");
            return;
        }
        for (int i = 0; i < booked.size(); i++) println(String.format(" %d. %s", i + 1, describe(booked.get(i))));
        int n = askInt("Appointment to cancel", 1, booked.size());
        Appointment a = booked.get(n - 1);
        a.cancel();
        log.record(staffName, "appointment cancelled", p);
        println("Cancelled. The time slot is open again.");
    }

    // ------------------------------------------------------------------ 7-8. CSV (SO31, SO32, Part A)

    private void importCsv() {
        println("The system starts fresh. Import in this order: 1 availability, 2 booked appointments, 3 waiting list patients.");
        println("Press Enter on a path to use the sample file in data/ (run from iteration2/poc).");
        importOne("Availability CSV", "data/professional-availability.csv", 1);
        importOne("Booked appointments CSV", "data/booked-appointments.csv", 2);
        importOne("Waiting list patients CSV", "data/waitlist-patients.csv", 3);
    }

    private void importOne(String label, String defaultPath, int kind) {
        String s = ask(label + " path (Enter = " + defaultPath + ", \"skip\" to skip)");
        if (s == null) throw new EndOfInput();
        if (s.equalsIgnoreCase("skip")) return;
        Path path = Path.of(s.isBlank() ? defaultPath : s);
        try {
            ImportResult r = switch (kind) {
                case 1 -> csv.importAvailability(path);
                case 2 -> csv.importAppointments(path);
                default -> csv.importWaitlistPatients(path);
            };
            println(label + ": " + r);
            for (String error : r.getErrors()) println("  " + error);
        } catch (IOException e) {
            println(label + " not imported: " + e.getMessage());
        }
    }

    private void exportCsv() {
        String path = askRequired("Export to a NEW file (for example availability-export.csv)");
        try {
            int n = csv.exportAvailability(Path.of(path));
            println(n + " slot(s) exported to " + Path.of(path).toAbsolutePath().normalize());
        } catch (IOException e) {
            println("Not exported: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ 9-10. Information

    private void showWaitlistInfo() {
        List<WaitlistPatientInfo> info = csv.getWaitlistPatientInfo();
        if (info.isEmpty()) println("No waiting list information imported yet (menu 7).");
        for (WaitlistPatientInfo i : info) println("  " + i);
    }

    private void showLog() {
        List<ActivityRecord> all = log.getAll();
        if (all.isEmpty()) println("No activity yet.");
        for (ActivityRecord r : all) println(r.toString());
    }

    // ------------------------------------------------------------------ choosing professionals, specialties, locations

    private List<HealthcareProfessional> sortedProfessionals() {
        List<HealthcareProfessional> all = new ArrayList<>(professionals.getAll());
        all.sort(Comparator.comparing(HealthcareProfessional::getFullName));
        return all;
    }

    private HealthcareProfessional chooseProfessional(boolean optional) {
        List<HealthcareProfessional> all = sortedProfessionals();
        if (!all.isEmpty()) {
            println("Healthcare professionals:");
            for (int i = 0; i < all.size(); i++) {
                println(String.format(" %d. %s %s", i + 1, all.get(i), all.get(i).getSpecialties()));
            }
        }
        while (true) {
            String s = optional ? ask("Professional number (empty = any)") : askRequired("Professional number, or \"new\"");
            if (s == null) throw new EndOfInput();
            if (s.isBlank()) return null;
            if (!optional && s.equalsIgnoreCase("new")) {
                String title = ask("Title (Dr., Nurse Practitioner...)");
                if (title == null) throw new EndOfInput();
                HealthcareProfessional h = professionals.findOrCreate(title, askRequired("First name"),
                        askRequired("Last name"), title);
                String spec = ask("Specialty (empty = none)");
                if (spec != null && !spec.isBlank()) h.addSpecialty(new MedSpecialty(spec));
                return h;
            }
            try {
                int i = Integer.parseInt(s.trim());
                if (i >= 1 && i <= all.size()) return all.get(i - 1);
            } catch (NumberFormatException ignored) { }
            println("Type a number from the list" + (optional ? " or leave it empty." : " or \"new\"."));
        }
    }

    private MedSpecialty chooseSpecialty() {
        Set<MedSpecialty> set = new LinkedHashSet<>();
        for (HealthcareProfessional h : sortedProfessionals()) set.addAll(h.getSpecialties());
        if (set.isEmpty()) return null;
        List<MedSpecialty> list = new ArrayList<>(set);
        println("Specialties:");
        for (int i = 0; i < list.size(); i++) println(String.format(" %d. %s", i + 1, list.get(i)));
        while (true) {
            String s = ask("Specialty number (empty = any)");
            if (s == null) throw new EndOfInput();
            if (s.isBlank()) return null;
            try {
                int i = Integer.parseInt(s.trim());
                if (i >= 1 && i <= list.size()) return list.get(i - 1);
            } catch (NumberFormatException ignored) { }
            println("Type a number from the list or leave it empty.");
        }
    }

    private ClinicLocation findLocation(String name) {
        if (name == null || name.isBlank() || name.trim().equalsIgnoreCase(mainClinic.getName())) return mainClinic;
        for (TimeSlot s : availability.getAllSlots()) {
            if (s.getLocation().getName().equalsIgnoreCase(name.trim())) return s.getLocation();
        }
        return new ClinicLocation(name.trim(), null, null);
    }

    // ------------------------------------------------------------------ display helpers

    private static String describe(Patient p) {
        String ramq = p.getRamqNumber();
        return "#" + p.getPatientNumber() + " " + p.getFullName()
                + (p.getDateOfBirth() == null ? "" : ", born " + p.getDateOfBirth())
                + ", phone " + p.getPhone()
                + (ramq == null ? ", no RAMQ" : ", RAMQ " + ramq);
    }

    private static String describe(TimeSlot s) {
        return s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime() + " | " + s.getProfessional()
                + " " + s.getProfessional().getSpecialties() + " | " + s.getLocation();
    }

    private static String describe(Appointment a) {
        return describe(a.getSlot()) + " | " + a.getStatus() + " | patient #" + a.getPatient().getPatientNumber()
                + " " + a.getPatient().getFullName();
    }

    // ------------------------------------------------------------------ input helpers (bad input never crashes)

    private static final class EndOfInput extends RuntimeException { }

    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            println("");
            return null;
        }
        return in.nextLine().trim();
    }

    private String askRequired(String prompt) {
        while (true) {
            String s = ask(prompt);
            if (s == null) throw new EndOfInput();
            if (!s.isBlank()) return s;
            println("This field is required.");
        }
    }

    private LocalDate askDate(String prompt, boolean optional) {
        while (true) {
            String s = optional ? ask(prompt) : askRequired(prompt);
            if (s == null) throw new EndOfInput();
            if (s.isBlank()) return null;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                println("Use the format YYYY-MM-DD, for example 2026-11-23.");
            }
        }
    }

    private LocalTime askTime(String prompt) {
        while (true) {
            String s = askRequired(prompt);
            try {
                return LocalTime.parse(s.length() == 4 ? "0" + s : s);
            } catch (DateTimeParseException e) {
                println("Use the format HH:MM, for example 09:00.");
            }
        }
    }

    private int askInt(String prompt, int min, int max) {
        while (true) {
            String s = askRequired(prompt);
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    private Set<DayOfWeek> askDays() {
        while (true) {
            String s = askRequired("Days (for example MON,WED)");
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            boolean ok = true;
            for (String part : s.split("[,\\s]+")) {
                DayOfWeek d = parseDay(part);
                if (d == null) { ok = false; break; }
                days.add(d);
            }
            if (ok && !days.isEmpty()) return days;
            println("Use day names such as MON, TUE, WED, THU, FRI, SAT, SUN.");
        }
    }

    private static DayOfWeek parseDay(String s) {
        String u = s.trim().toUpperCase(Locale.ROOT);
        if (u.length() < 3) return null;
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d.name().startsWith(u.substring(0, 3))) return d;
        }
        return null;
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
