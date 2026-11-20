---
title: "Operation contracts CO1 to CO3 and CO18 (UC1 Request Appointment, revised for Iteration 2)"
subtitle: "SOEN 342 Appointment Tracking System, Part C (Ryan Helou)"
date: "Version 2.0, November 19, 2026 (replaces CO1 to CO3 from Iteration 1)"
---

Contracts describe conceptual state changes in terms of the updated domain model (iteration2/analysis/domain-model.svg), in the same format as the Iteration 1 contracts and Part A's operation-contracts.md. SO01 corresponds to CO1, SO02 to CO2, SO03 to CO3. Instances, attributes and associations not named are unchanged. M = main step, E = extension of the revised UC1 (UC1-request-appointment-v2.pdf). SSDs: ssd/so01-so03-uc1-request-appointment-*.

# System operations of UC1

| ID | Operation | Origin | Outcome and persistent state |
|----|-----------|--------|------------------------------|
| SO01 | startBooking(patientKey: Text) | UC1 M1-M2; E1a; E2a. SSD UC1-S, UC1-F | Identify the Patient for the booking. State: no. |
| SO26 | searchAvailability(criteria: SearchCriteria) | UC1 M3 (include Search Availability); E3a. Part B | Show open TimeSlots. State: no. |
| SO02 | bookTimeSlot(timeSlot: TimeSlotRef) | UC1 M4-M8; E5a; E5b. SSD UC1-S, UC1-F | Record a BOOKED Appointment, use up the TimeSlot, audit. State: yes. |
| SO03 | cancelBooking() | UC1 E\*a. SSD UC1-F | Discard the unfinished booking. State: no. |
| SO18 | registerPatient(firstName, lastName, dateOfBirth, phone, insurance: Optional\<InsuranceInput\>) | NC3 Register Patient; UC1 E1a | Create Patient, ClinicalRecord, optional Insurance; audit. State: yes. |

Parameter types: Text is a value. patientKey is either a phone number or a Patient.patientNumber. TimeSlotRef identifies one TimeSlot presented by searchAvailability; it may be stale. SearchCriteria holds an optional HealthcareProfessional, MedSpecialty, fromDate and toDate. These interface types do not add domain classes.

# Contract CO1: startBooking

**Operation:** startBooking(patientKey: Text)

**Cross references:** UC1 Request Appointment, M1-M2; E1a.1; E2a.1-2. SO01.

**Preconditions:**

- The Administrative Staff member is identified.
- No other booking is in progress for this staff member.

**Postconditions:**

1. Instances: none created or deleted.
2. Attributes: none modified.
3. Associations: none formed or broken.

**Failures:** E1a: no Patient has phone = patientKey or patientNumber = patientKey. The System reports "patient not found"; nothing changes; the staff member may run registerPatient (SO18, CO18) and the booking continues with the new Patient. E2a: several Patients have phone = patientKey. The System returns the list of matching Patients (patient number and name); nothing changes; the staff member calls startBooking again with one patientNumber.

**Notes:** On success the matched Patient p becomes the patient of the unfinished booking (interaction context, not persistent state) and the System returns p's name and patientNumber. This is a query operation, so it has no postconditions on domain state. Patients without a RAMQ number are found by phone or patientNumber.

# Contract CO2: bookTimeSlot

**Operation:** bookTimeSlot(timeSlot: TimeSlotRef)

**Cross references:** UC1 Request Appointment, M4-M8; E5a.1-2; E5b.1-2. SO02. Follows SO26 searchAvailability (include Search Availability, M3).

**Preconditions:**

- SO01 succeeded: an unfinished booking exists for Patient p.
- timeSlot identifies an existing TimeSlot ts, of HealthcareProfessional h at ClinicLocation l, that was presented by searchAvailability.

**Postconditions (success):**

1. Instances: an Appointment a was created. An ActivityRecord ar was created.
2. Attributes:
   - a.status became BOOKED (there is no Requested status).
   - ts.status became BOOKED (it was OPEN).
   - ar.timestamp became the current date and time; ar.action became "appointment booked"; ar.performedBy became the identified staff member.
3. Associations:
   - p was associated with a (Patient *books* Appointment).
   - a was associated with ts (Appointment *assigned to* TimeSlot).
   - a was associated with h (Appointment *is booked for* HealthcareProfessional) and with l (Appointment *held at* ClinicLocation), the same HealthcareProfessional and ClinicLocation as ts.
   - ar was associated with p (ActivityRecord *concerns* Patient).
4. The unfinished booking ended.

**Failures:** E5a: ts.status is not OPEN (booked by someone else after it was presented). No Appointment is created and ts is unchanged; the System reports that the TimeSlot is no longer available and the booking stays open so the staff member can search again (M3). E5b: p already books an Appointment that is not cancelled and whose TimeSlot overlaps ts in time (same date, intersecting [start, end)). No Appointment is created; the System returns the conflicting Appointment; the booking stays open (M3).

**Notes:** Booking uses up the TimeSlot: after this operation searchAvailability no longer returns ts. Cancelling a recorded Appointment later (revised CO17 cancelAppointment, Part A) sets ts.status back to OPEN. Referral checks are not part of this contract (see UC1 open issues). When resources were selected through E4a (CO34, analysis only), the resource allocations are formed together with the Appointment; this is Iteration III design.

# Contract CO3: cancelBooking

**Operation:** cancelBooking()

**Cross references:** UC1 Request Appointment, E\*a.1. SO03.

**Preconditions:**

- An unfinished booking exists (SO01 succeeded) and SO02 has not yet succeeded for it.

**Postconditions:**

1. Instances: none created or deleted. No Appointment was recorded.
2. Attributes: none modified. Every TimeSlot kept its status.
3. Associations: none formed or broken.
4. The unfinished booking was discarded.

**Failures:** none documented. Cancelling an Appointment that was already booked is a different operation (SO17 cancelAppointment).

**Notes:** Nothing is held by an unfinished booking (the TimeSlot is only used up in SO02), so there is nothing to release.

# Contract CO18: registerPatient (used by UC1 extension 2a)

**Operation:** registerPatient(firstName: Text, lastName: Text, dateOfBirth: Date, phone: Text, insurance: Optional\<InsuranceInput\>)

**Cross references:** NC3 Register Patient; UC1 E1a.1. SO18 (Iteration 1 number).

**Preconditions:** the Administrative Staff member is identified.

**Postconditions:**

1. Instances: a Patient p was created. If insurance was given, an Insurance i was created. An ActivityRecord ar was created.
2. Attributes: p.firstName, p.lastName, p.dateOfBirth and p.phone became the given values; p.patientNumber became a new unique number. If given, i.type, i.number and i.provider became the given values. ar.timestamp, ar.action ("patient registered") and ar.performedBy were set.
3. Associations: if given, p was associated with i (Patient *has* Insurance, 0..\*). ar was associated with p (*concerns*).

**Failures:** a Patient with the same first name, last name and date of birth already exists: nothing is created and the existing Patient is returned. Invalid insurance (RAMQ without number, private without provider): nothing is created.

**Notes:** RAMQ is optional because some Patients have no RAMQ number. NC3 in Iteration 1 also creates an empty ClinicalRecord; UC2 is design-only in Iteration II, so the PoC does not create ClinicalRecords yet (Iteration III).

# Changes from Iteration 1

| Iteration 1 | Iteration 2 | Why |
|-------------|-------------|-----|
| CO1 beginAppointmentRequest(medicalService) | CO1 startBooking(patientKey) | Staff identify the Patient first; choosing times moved into the included Search Availability (SO26). |
| CO2 selectAppointmentTime(professional, location, dateTime), status Requested | CO2 bookTimeSlot(timeSlot), status BOOKED, TimeSlot status BOOKED | Booked directly; availability is explicit TimeSlots that a booking uses up. |
| CO3 cancelAppointmentRequest() | CO3 cancelBooking() | Same meaning, renamed to match the use case. |
| Referral check in CO1 | Moved to UC1 open issues | TimeSlots do not carry a MedicalService in the PoC. |
| Save-failure branch E6a | Removed | Not meaningful in the in-memory PoC. |
