# UC1 Request Appointment realizations (SO01, SO02, SO03, SO18)

One design sequence diagram per UC1 system operation, drawn from the revised contracts in [../../analysis/UC1-operation-contracts.pdf](../../analysis/UC1-operation-contracts.pdf) and the revised use case [../../analysis/UC1-request-appointment-v2.pdf](../../analysis/UC1-request-appointment-v2.pdf). Source is Mermaid (`.mmd`), exported to `.svg` and `.png`. Notation follows the course decks: no activation bars, guards in brackets, `create` messages start new lifelines, `alt`/`opt`/`loop` fragments for the extensions.

| Diagram | System operation | Contract | UC1 steps |
|---------|------------------|----------|-----------|
| [so01-start-booking](so01-start-booking.svg) | `startBooking(patientKey)` | CO1 | M1-M2, E1a, E2a |
| [so02-book-time-slot](so02-book-time-slot.svg) | `bookTimeSlot(timeSlot)` | CO2 | M4-M8, E5a, E5b |
| [so03-cancel-booking](so03-cancel-booking.svg) | `cancelBooking()` | CO3 | E*a |
| [so18-register-patient](so18-register-patient.svg) | `registerPatient(firstName, lastName, dateOfBirth, phone, insurance)` | CO18 | E1a (NC3 Register Patient) |

M3 is the included Search Availability (SO26, Part B): `AvailabilityService.searchAvailability(SearchCriteria)`. It is not repeated here.

## Contract guarantee to realization

| Contract | Guarantee | Realization |
|----------|-----------|-------------|
| CO1 | No domain change; one match starts the booking for p | `PatientRegistry.findByKey` checks `getPatientNumber()` and `getPhone()`; `BookingController.setCurrentPatient(p)` |
| CO1 E1a / E2a | Nothing changes | empty or several matches: `setCurrentPatient(null)` |
| CO2 | Appointment a created | `AppointmentRegistry.book(p, ts)` creates `a : Appointment` |
| CO2 | a.status became BOOKED | `Appointment` constructor (Part B) |
| CO2 | ts.status became BOOKED | `Appointment` sends `ts.book()` (Part B; package-private, so only an Appointment can occupy a slot) |
| CO2 | p books a; a assigned to ts (and so to h and l) | constructor arguments `(p, ts)`; the TimeSlot holds the HealthcareProfessional and ClinicLocation |
| CO2 | ActivityRecord ar created, concerns p | `ActivityLog.record(staffName, "appointment booked", p)` creates `ar : ActivityRecord` |
| CO2 E5a | Nothing created when ts is not open | `ts.isOpen()` is false: `SlotUnavailableException` |
| CO2 E5b | Nothing created when p has an overlapping non-cancelled Appointment | `AppointmentRegistry.findOverlapping(p, ts)`, time overlap on the same date with any professional |
| CO2 | Booking ended | `setCurrentPatient(null)` |
| CO3 | Nothing created or changed; booking discarded | `setCurrentPatient(null)` only |
| CO18 | Patient p created, patientNumber automatic | `PatientRegistry.register` creates `p : Patient` |
| CO18 | Insurance linked (optional, 0..*) | `p.addInsurance(insurance)` |
| CO18 | ActivityRecord created | `ActivityLog.record(staffName, "patient registered", p)` |
| CO18 failure | Existing Patient returned, nothing created | `findDuplicate(firstName, lastName, dateOfBirth)` |

## GRASP decisions

| Responsibility | Assigned to | GRASP |
|----------------|-------------|-------|
| Receive SO01, SO02, SO03, SO18 from the UI | `BookingController`, a use case controller in the same style as `ConsultationController` (UC2) and `ReferralController` (UC3) | Controller |
| Keep the patient of the unfinished booking between operations | `BookingController.currentPatient` | Controller (use case session state) |
| Find Patients by phone or patient number | `PatientRegistry`, which holds every Patient | Information Expert, Pure Fabrication |
| Create a Patient | `PatientRegistry`, which records Patients | Creator |
| Create an Appointment | `AppointmentRegistry`, which records Appointments and receives `p` and `ts` | Creator |
| Say whether a slot is open; mark it booked | `TimeSlot`, which owns its status | Information Expert |
| Find the Patient's overlapping Appointment | `AppointmentRegistry`, which knows every Appointment | Information Expert |
| Create an ActivityRecord | `ActivityLog` (same class and parameter order as UC2: `record(user, action, patient)`) | Creator, Pure Fabrication, High Cohesion |
| Search availability | stays on `AvailabilityService` (Part B); UC1 includes it and does not duplicate it | Information Expert, Low Coupling |

The UI (`ConsoleMenu`) holds no domain state of its own beyond the shared collections it gives to the registries and to Part A's `CsvImportExport`, so imported and booked data are the same objects (Low Coupling).

## Visibility

| Sender to receiver | Visibility |
|--------------------|------------|
| BookingController to PatientRegistry, AppointmentRegistry, ActivityLog | attribute (constructor) |
| BookingController to ts : TimeSlot | parameter |
| BookingController to p : Patient | attribute (`currentPatient`) |
| PatientRegistry to p : Patient | local (loop over its list) |
| AppointmentRegistry to x : Appointment | local (loop over its list) |
| AppointmentRegistry to a : Appointment | local (created) |
| Appointment to ts : TimeSlot | parameter of the constructor, then attribute |
| ActivityLog to ar : ActivityRecord | local (created) |
