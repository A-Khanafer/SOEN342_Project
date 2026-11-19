# SOEN342_Project: Appointment Tracking System

- Ahmad Khanafer (40312768)
- Elias Nasrallah (40233118)
- Ryan Helou (40315563)

## Iteration II: where everything is

| Handout deliverable | Location |
|---------------------|----------|
| 1. Updated use case diagram (critical and non-critical) | [iteration2/analysis/use-case-diagram.svg](iteration2/analysis/use-case-diagram.svg), scenarios in [use-case-scenarios.md](iteration2/analysis/use-case-scenarios.md), impact analysis in [impact-analysis.md](iteration2/analysis/impact-analysis.md) |
| 2. Updated domain model | [iteration2/analysis/domain-model.svg](iteration2/analysis/domain-model.svg) (+ waitlist and resource views) |
| 3. Updated SSDs | [iteration2/analysis/ssd/](iteration2/analysis/ssd/): UC1 SO01-SO03 (Part C), SO25-SO26 (Part B), SO17 and SO27-SO34 (Part A) |
| Revised UC1 and its contracts | [UC1-request-appointment-v2.pdf](iteration2/analysis/UC1-request-appointment-v2.pdf), [UC1-operation-contracts.pdf](iteration2/analysis/UC1-operation-contracts.pdf); other contracts in [operation-contracts.md](iteration2/analysis/operation-contracts.md) |
| 4. Interaction diagrams for the 3 critical use cases | [design/uc1](iteration2/design/uc1/) (SO01-SO03, SO18), [design/uc2](iteration2/design/uc2/) (SO04-SO07), [design/uc3](iteration2/design/uc3/) (SO08-SO10); each folder has a README with GRASP notes |
| 5. Design class diagram (partial design model) | [iteration2/design/class-diagram/design-class-diagram.svg](iteration2/design/class-diagram/design-class-diagram.svg); cross-review in [iteration2/design/cross-review.md](iteration2/design/cross-review.md) |
| 6. PoC (Java) | [iteration2/poc](iteration2/poc/) |
| Iteration I fully dressed scenarios as PDF (TA feedback) | [iteration1/](iteration1/) |

Critical use cases: UC1 Request Appointment (booking by Administrative Staff), UC2 Conduct Consultation, UC3 Issue Referral. Non-critical: NC1-NC9, Make Availability, Search Availability, W1-W4, C1-C2, R1 (see the use case diagram).

## Running the PoC

Java 17 or later, plain `javac` (no Maven). From the repository root:

```bash
cd iteration2/poc
javac -d out $(find src -name "*.java")
java -cp out ats.ui.ConsoleMenu
```

Run it from `iteration2/poc` so the sample files in `data/` are found. `java -cp out ats.App` runs Part B's availability self-check and `java -cp out ats.io.CsvDemo` runs Part A's CSV demonstration.

Tests (plain Java, no JUnit):

```bash
cd iteration2/poc
javac -d out $(find src test -name "*.java")
java -cp out ats.service.BookingControllerTest
java -cp out ats.service.CsvBookingIntegrationTest
```

## Menu

| Option | Feature (handout) | System operation |
|--------|-------------------|------------------|
| 1 | Making Availability | SO25 createAvailability |
| 2 | Searching for Availability | SO26 searchAvailability |
| 3 | Booking an appointment (at the clinic or by phone) | SO01 startBooking, SO26, SO02 bookTimeSlot, SO03 cancelBooking |
| 4 | Register a patient | SO18 registerPatient |
| 5 | Find a patient by phone or patient number | SO01 lookup |
| 6 | Cancel an appointment (frees the slot) | SO17 cancelAppointment |
| 7 | Import from CSV (availability, then booked appointments, then waiting list) | SO31 importCsv |
| 8 | Export to CSV (the whole availability database) | SO32 exportAvailabilityCsv |
| 9 | Show imported waiting list information | SO31 result |
| 10 | Activity log | ActivityRecords from SO02, SO17, SO18 |

## Demo script (about 5 minutes)

1. **Import from CSV** (7): press Enter three times to load the handout's sample files in order. Result: 8 slots, 4 booked appointments, 5 waiting list patients.
2. **Search availability** (2): press Enter at every prompt. 4 open slots (the 4 booked ones are not shown).
3. **Book for a new patient on the phone** (3): type `999-999-9999`: "No patient found". Type `new`, register Ana Lopez, 1990-05-02, 514-555-7777, no insurance (no RAMQ). Press Enter at the search prompts and pick slot 1. Booked.
4. **The slot is used up** (2): search again: 3 open slots.
5. **Book for a patient at the desk, found by phone** (3): type `438-555-0103` (Fatima Benali, imported, no RAMQ). Pick a slot.
6. **Make availability** (1): `new`, Dr. Xavier Roy, specialty Cardiology, Enter for Main Clinic, 2026-11-01 to 2026-11-30, pattern 2, `MON`, 09:00 to 12:00, 30 minute slots: 30 slots ("during November, Dr. X is available every Monday 9am to 12pm").
7. **Overlap refused** (3): make availability for another doctor on 2026-11-02 from 09:00 to 10:00, then book Ana (514-555-7777) and pick that doctor's 09:30 slot. Refused: she already has an appointment at that time.
8. **Cancel** (6): type `514-555-7777`, cancel her appointment. Search (2): the slot is open again.
9. **Export to CSV** (8): type `availability-export.csv` and open the file: every slot, with BOOKED rows showing the patient. Exporting to the same name again is refused (no overwrite).
10. **Waiting list information** (9) and **activity log** (10).
11. **Bad input**: type `42`, an empty line, `2026-13-40`, `9h` or `abc` at any prompt. The program explains the format and continues.

## Part C traceability (UC1)

| UC1 step | System operation | Contract | Diagram | Code |
|----------|------------------|----------|---------|------|
| M1-M2, E1a, E2a | SO01 startBooking(patientKey) | CO1 | design/uc1/so01-start-booking | `BookingController.startBooking`, `PatientRegistry.findByKey` |
| M3, E3a (include Search Availability) | SO26 searchAvailability(criteria) | Part B | Part B SSD | `AvailabilityService.searchAvailability` |
| M4-M8, E5a, E5b | SO02 bookTimeSlot(timeSlot) | CO2 | design/uc1/so02-book-time-slot | `BookingController.bookTimeSlot`, `AppointmentRegistry.findOverlapping / book`, `Appointment(Patient, TimeSlot)`, `TimeSlot.isOpen / book`, `ActivityLog.record` |
| E*a | SO03 cancelBooking() | CO3 | design/uc1/so03-cancel-booking | `BookingController.cancelBooking` |
| E1a (NC3 Register Patient) | SO18 registerPatient(...) | CO18 | design/uc1/so18-register-patient | `BookingController.registerPatient`, `PatientRegistry.register`, `Patient.addInsurance` |
