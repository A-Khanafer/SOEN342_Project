package ats.io;

import ats.domain.Appointment;
import ats.domain.AppointmentStatus;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.Insurance;
import ats.domain.InsuranceType;
import ats.domain.MedSpecialty;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.service.AvailabilityService;
import ats.service.ProfessionalRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** In-memory CSV integration. Supply the SAME registry, availability and mutable lists as the menu. */
public final class CsvImportExport {
    private static final String[] AVAILABILITY = {"First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End"};
    private static final String[] APPOINTMENTS = {"Professional", "Date", "Start", "End", "Patient", "Telephone", "RAMQ No."};
    private static final String[] WAITLIST = {"First Name", "Last Name", "Telephone", "RAMQ", "Insurance", "Referral Type", "Description"};
    private static final String[] EXPORT = {"First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End",
            "Location", "Profession Type", "Status", "Patient", "Patient First Name", "Patient Last Name",
            "Telephone", "RAMQ No.", "Insurance", "Insurance No."};

    private final ProfessionalRegistry professionals;
    private final AvailabilityService availability;
    private final ClinicLocation defaultLocation;
    private final List<Patient> patients;
    private final List<Appointment> appointments;
    private final List<WaitlistPatientInfo> waitlistInfo = new ArrayList<>();

    public CsvImportExport(ProfessionalRegistry professionals, AvailabilityService availability,
                           ClinicLocation defaultLocation, List<Patient> patients, List<Appointment> appointments) {
        this.professionals = Objects.requireNonNull(professionals);
        this.availability = Objects.requireNonNull(availability);
        this.defaultLocation = Objects.requireNonNull(defaultLocation);
        this.patients = Objects.requireNonNull(patients);
        this.appointments = Objects.requireNonNull(appointments);
    }

    public ImportResult importAvailability(Path source) throws IOException {
        List<Map<String, String>> rows = CsvFile.read(source, AVAILABILITY, "Location", "Profession Type", "Status",
                "Patient", "Patient First Name", "Patient Last Name", "Telephone", "RAMQ No.", "Insurance", "Insurance No.");
        return importRows(rows, (row, result) -> {
            String first = required(row, "First Name"), last = required(row, "Last Name");
            List<MedSpecialty> specialties = new ArrayList<>();
            if (!value(row, "Specialty").isEmpty()) {
                for (String name : value(row, "Specialty").split(";", -1)) specialties.add(new MedSpecialty(name));
            }
            LocalDate date = date(row);
            LocalTime start = time(row, "Start"), end = time(row, "End");
            interval(start, end);
            ClinicLocation location = location(value(row, "Location"));
            String status = value(row, "Status").toUpperCase(Locale.ROOT);
            if (!status.isEmpty() && !status.equals("OPEN") && !status.equals("BOOKED")) {
                throw new IllegalArgumentException("Status must be OPEN or BOOKED");
            }
            PatientInput input = status.equals("BOOKED") ? patientInput(row, false) : null;
            if (input == null && (!value(row, "Patient").isEmpty() || !value(row, "RAMQ No.").isEmpty()
                    || !value(row, "Telephone").isEmpty() || !value(row, "Insurance").isEmpty()
                    || !value(row, "Insurance No.").isEmpty() || !value(row, "Patient First Name").isEmpty()
                    || !value(row, "Patient Last Name").isEmpty())) {
                throw new IllegalArgumentException("Patient information requires Status=BOOKED");
            }
            HealthcareProfessional professional = professionals.find(first, last);
            TimeSlot existing = professional == null ? null : matchingSlot(professional, date, start, end, location.getName());
            if (existing != null && status.equals("OPEN") && !existing.isOpen()) {
                throw new IllegalArgumentException("OPEN CSV row conflicts with an occupied slot; cancel through Appointment");
            }
            if (existing == null && professional != null) {
                TimeSlot candidate = new TimeSlot(professional, location, date, start, end);
                for (TimeSlot slot : availability.getAllSlots()) {
                    if (candidate.overlaps(slot)) throw new IllegalArgumentException("Availability overlaps existing slot: " + slot);
                }
            }
            if (input != null) validateBooking(input, existing, date, start, end);
            // All row values and conflicts have been checked before adding shared objects.
            if (professional == null) {
                String professionType = value(row, "Profession Type");
                professional = professionals.findOrCreate(value(row, "Title"), first, last,
                        professionType.isEmpty() ? value(row, "Title") : professionType);
            }
            TimeSlot slot = existing;
            if (slot == null) {
                slot = new TimeSlot(professional, location, date, start, end);
                availability.addSlot(slot);
                result.slotsImported++;
            }
            for (MedSpecialty specialty : specialties) professional.addSpecialty(specialty);
            if (input != null && slot.isOpen()) book(input, slot, result);
            if (existing != null && (input == null || !existing.isOpen()) && result.slotsImported == 0
                    && result.appointmentsImported == 0) return false;
            return true;
        });
    }

    public ImportResult importAppointments(Path source) throws IOException {
        return importRows(CsvFile.read(source, APPOINTMENTS, "Location", "Insurance", "Insurance No.",
                "Patient First Name", "Patient Last Name"), (row, result) -> {
            String name = required(row, "Professional");
            HealthcareProfessional professional = null;
            for (HealthcareProfessional candidate : professionals.getAll()) {
                if (key(candidate.getFullName()).equals(key(name))) {
                    if (professional != null) throw new IllegalArgumentException("Ambiguous professional name: " + name);
                    professional = candidate;
                }
            }
            if (professional == null) throw new IllegalArgumentException("Import availability first; professional not found: " + name);
            LocalDate date = date(row);
            LocalTime start = time(row, "Start"), end = time(row, "End");
            interval(start, end);
            TimeSlot slot = matchingSlot(professional, date, start, end, value(row, "Location"));
            if (slot == null) throw new IllegalArgumentException("No matching imported slot for " + name + " " + date + " " + start);
            PatientInput input = patientInput(row, false);
            validateBooking(input, slot, date, start, end);
            if (!slot.isOpen()) return false; // Same patient/slot was already imported.
            book(input, slot, result);
            return true;
        });
    }

    public ImportResult importWaitlistPatients(Path source) throws IOException {
        return importRows(CsvFile.read(source, WAITLIST, "No.", "Insurance No."), (row, result) -> {
            PatientInput input = patientInput(row, true);
            String type = required(row, "Referral Type"), description = required(row, "Description");
            for (WaitlistPatientInfo info : waitlistInfo) {
                if (info.getPatient() == input.existing && info.getReferralType().equals(type)
                        && info.getDescription().equals(description)) return false;
            }
            Patient patient = materialize(input, result);
            waitlistInfo.add(new WaitlistPatientInfo(patient, type, description));
            result.waitlistInfoImported++;
            return true;
        });
    }

    /** Full slot snapshot, including occupancy. Existing destinations are rejected, not overwritten. */
    public int exportAvailability(Path destination) throws IOException {
        if (destination == null) throw new IOException("Export file path is required");
        Path target = destination.toAbsolutePath().normalize();
        if (target.getFileName() == null || !Files.isDirectory(target.getParent())
                || !Files.isWritable(target.getParent()) || Files.exists(target)) {
            throw new IOException("Export needs a new file in an existing writable directory: " + target);
        }
        StringBuilder csv = new StringBuilder(CsvFile.record(Arrays.asList(EXPORT)));
        for (TimeSlot slot : availability.getAllSlots()) {
            Appointment booking = bookingFor(slot);
            if (slot.isOpen() && booking != null || !slot.isOpen() && booking == null) {
                throw new IOException("Slot status and supplied appointment list disagree: " + slot);
            }
            Patient patient = booking == null ? null : booking.getPatient();
            if (patient != null && digits(patient.getPhone()).isEmpty()) {
                throw new IOException("Booked patient needs a telephone for reusable CSV export: " + patient);
            }
            String provider = "", number = "";
            if (patient != null) {
                for (Insurance insurance : patient.getInsurances()) {
                    if (insurance.getType() == InsuranceType.PRIVATE) {
                        if (!provider.isEmpty()) throw new IOException("CSV supports one private insurance per booked patient");
                        provider = insurance.getProvider();
                        number = insurance.getNumber();
                    }
                }
            }
            HealthcareProfessional p = slot.getProfessional();
            csv.append(CsvFile.record(Arrays.asList(p.getfName(), p.getlName(), p.getTitle(),
                    p.getSpecialties().stream().map(MedSpecialty::getName).collect(Collectors.joining(";")),
                    slot.getDate().toString(), slot.getStartTime().toString(), slot.getEndTime().toString(),
                    slot.getLocation().getName(), p.getPType(), slot.getStatus().name(),
                    patient == null ? "" : patient.getFullName(), patient == null ? "" : patient.getFirstName(),
                    patient == null ? "" : patient.getLastName(), patient == null ? "" : patient.getPhone(),
                    patient == null ? "" : patient.getRamqNumber(), provider, number)));
        }
        Files.writeString(target, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return availability.getAllSlots().size();
    }

    public List<Patient> getPatients() { return Collections.unmodifiableList(patients); }
    public List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }
    public List<WaitlistPatientInfo> getWaitlistPatientInfo() { return Collections.unmodifiableList(waitlistInfo); }

    private interface RowImporter { boolean apply(Map<String, String> row, ImportResult result); }

    private ImportResult importRows(List<Map<String, String>> rows, RowImporter importer) {
        ImportResult total = new ImportResult();
        for (int i = 0; i < rows.size(); i++) {
            ImportResult rowResult = new ImportResult();
            try {
                if (importer.apply(rows.get(i), rowResult)) total.rowsImported++;
                else total.rowsSkipped++;
                total.slotsImported += rowResult.slotsImported;
                total.appointmentsImported += rowResult.appointmentsImported;
                total.patientsImported += rowResult.patientsImported;
                total.waitlistInfoImported += rowResult.waitlistInfoImported;
            } catch (IllegalArgumentException | IllegalStateException e) {
                total.errors.add("Record " + (i + 2) + ": " + e.getMessage());
            }
        }
        return total;
    }

    private TimeSlot matchingSlot(HealthcareProfessional professional, LocalDate date, LocalTime start,
                                  LocalTime end, String locationName) {
        TimeSlot found = null;
        for (TimeSlot slot : availability.getAllSlots()) {
            if (slot.getProfessional().equals(professional) && slot.getDate().equals(date)
                    && slot.getStartTime().equals(start) && slot.getEndTime().equals(end)
                    && (locationName.isBlank() || key(slot.getLocation().getName()).equals(key(locationName)))) {
                if (found != null) throw new IllegalArgumentException("Ambiguous slot; supply Location");
                found = slot;
            }
        }
        return found;
    }

    private Appointment bookingFor(TimeSlot slot) {
        Appointment found = null;
        for (Appointment appointment : appointments) {
            if (appointment.getSlot() == slot && appointment.getStatus() != AppointmentStatus.CANCELLED) {
                if (found != null) throw new IllegalStateException("Multiple active appointments occupy the same slot");
                found = appointment;
            }
        }
        return found;
    }

    private void validateBooking(PatientInput input, TimeSlot slot, LocalDate date, LocalTime start, LocalTime end) {
        if (slot != null && !slot.isOpen()) {
            Appointment booking = bookingFor(slot);
            if (booking == null || booking.getPatient() != input.existing) {
                throw new IllegalArgumentException("Slot is already occupied by another or unknown patient");
            }
            return;
        }
        if (input.existing != null) {
            for (Appointment appointment : appointments) {
                TimeSlot other = appointment.getSlot();
                if (appointment.getPatient() == input.existing && appointment.getStatus() != AppointmentStatus.CANCELLED
                        && other.getDate().equals(date) && start.isBefore(other.getEndTime()) && other.getStartTime().isBefore(end)) {
                    throw new IllegalArgumentException("Patient has an overlapping appointment");
                }
            }
        }
    }

    private void book(PatientInput input, TimeSlot slot, ImportResult result) {
        Patient patient = materialize(input, result);
        appointments.add(new Appointment(patient, slot)); // Part B owns the booking/status transition.
        result.appointmentsImported++;
    }

    private PatientInput patientInput(Map<String, String> row, boolean separateNames) {
        String first, last;
        if (separateNames) {
            first = required(row, "First Name");
            last = required(row, "Last Name");
        } else if (row.containsKey("Patient First Name") || row.containsKey("Patient Last Name")) {
            first = required(row, "Patient First Name");
            last = required(row, "Patient Last Name");
        } else {
            String[] name = required(row, "Patient").split("\\s+", 2);
            if (name.length != 2) throw new IllegalArgumentException("Patient needs first and last names");
            first = name[0]; last = name[1];
        }
        String phone = required(row, "Telephone");
        if (phone.replaceAll("\\D", "").isEmpty()) throw new IllegalArgumentException("Telephone must contain digits");
        String ramq = absent(value(row, separateNames ? "RAMQ" : "RAMQ No.")).toUpperCase(Locale.ROOT);
        String provider = absent(value(row, "Insurance")), number = absent(value(row, "Insurance No."));
        if (provider.isEmpty() && !number.isEmpty()) throw new IllegalArgumentException("Insurance No. requires Insurance provider");
        Patient existing = null;
        if (!ramq.isEmpty()) {
            for (Patient patient : patients) {
                if (ramq.equalsIgnoreCase(patient.getRamqNumber())) {
                    if (existing != null) throw new IllegalArgumentException("Duplicate RAMQ in shared patient list");
                    existing = patient;
                }
            }
            if (existing != null && (!key(existing.getFirstName()).equals(key(first))
                    || !key(existing.getLastName()).equals(key(last)))) {
                throw new IllegalArgumentException("RAMQ belongs to a different patient name");
            }
        }
        if (existing == null) {
            for (Patient patient : patients) {
                if (key(patient.getFirstName()).equals(key(first)) && key(patient.getLastName()).equals(key(last))
                        && digits(patient.getPhone()).equals(digits(phone))) {
                    if (existing != null) throw new IllegalArgumentException("Ambiguous name/telephone patient match");
                    if (!ramq.isEmpty() && patient.getRamqNumber() != null && !ramq.equalsIgnoreCase(patient.getRamqNumber())) {
                        throw new IllegalArgumentException("Conflicting RAMQ for name/telephone match");
                    }
                    existing = patient;
                }
            }
        }
        List<Insurance> insurance = new ArrayList<>();
        if (!ramq.isEmpty()) insurance.add(new Insurance(InsuranceType.RAMQ, ramq, "RAMQ"));
        if (!provider.isEmpty()) insurance.add(new Insurance(InsuranceType.PRIVATE, number, provider));
        return new PatientInput(first, last, phone, existing, insurance);
    }

    private Patient materialize(PatientInput input, ImportResult result) {
        Patient patient = input.existing;
        if (patient == null) {
            patient = new Patient(input.first, input.last, input.phone, null);
            patients.add(patient);
            result.patientsImported++;
        }
        for (Insurance candidate : input.insurance) {
            boolean duplicate = false;
            for (Insurance known : patient.getInsurances()) {
                if (known.getType() == candidate.getType() && key(known.getNumber()).equals(key(candidate.getNumber()))
                        && key(known.getProvider()).equals(key(candidate.getProvider()))) duplicate = true;
            }
            if (!duplicate) patient.addInsurance(candidate);
        }
        return patient;
    }

    private static final class PatientInput {
        final String first, last, phone;
        final Patient existing;
        final List<Insurance> insurance;
        PatientInput(String first, String last, String phone, Patient existing, List<Insurance> insurance) {
            this.first = first; this.last = last; this.phone = phone; this.existing = existing; this.insurance = insurance;
        }
    }

    private ClinicLocation location(String name) {
        if (name.isBlank() || key(name).equals(key(defaultLocation.getName()))) return defaultLocation;
        for (TimeSlot slot : availability.getAllSlots()) {
            if (key(slot.getLocation().getName()).equals(key(name))) return slot.getLocation();
        }
        return new ClinicLocation(name, null, null);
    }

    private static String value(Map<String, String> row, String name) { return row.getOrDefault(name, "").trim(); }
    private static String required(Map<String, String> row, String name) {
        String value = value(row, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
    private static String key(String value) { return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    private static String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private static String absent(String value) { return value.equals("-") || value.equals("–") || value.equals("—") ? "" : value; }
    private static LocalDate date(Map<String, String> row) {
        String value = required(row, "Date");
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Date must use yyyy-MM-dd");
        try { return LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Invalid Date: " + value); }
    }
    private static LocalTime time(Map<String, String> row, String field) {
        String value = required(row, field);
        if (!value.matches("[0-9]{2}:[0-9]{2}(:[0-9]{2}(\\.[0-9]{1,9})?)?")) {
            throw new IllegalArgumentException(field + " must use HH:mm with optional ISO seconds/fraction");
        }
        try { return LocalTime.parse(value); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Invalid " + field + ": " + value); }
    }
    private static void interval(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("End must be after Start on the same date");
    }
}
