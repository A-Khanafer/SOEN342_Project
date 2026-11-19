---
title: "UC1: Request Appointment (revised for Iteration 2: booked directly by Administrative Staff)"
subtitle: "SOEN 342 Appointment Tracking System, Part C (Ryan Helou)"
date: "Version 2.0, November 19, 2026. Replaces the Iteration 1 version (iteration1/UC1-request-appointment.pdf)."
---

**Scope:** Appointment Tracking System

**Level:** User goal

**Primary actor:** Administrative Staff

**Supporting actor:** Patient, at the clinic front desk or on the phone. The Patient asks for the appointment and agrees on the time; the Administrative Staff enters everything in the system.

**Related use cases:** includes Search Availability (SO26). Extended by W1 Add Patient to Waitlist (E3a) and R1 Inspect / Select Resources (E4a), as in the Iteration II use case diagram. Uses NC3 Register Patient for a new Patient (E1a).

# Stakeholders and interests

- **Administrative Staff:** wants to find the Patient quickly by phone number or patient number, see only open TimeSlots, and book in one pass while the Patient waits at the desk or on the line.
- **Patient:** wants an Appointment with the right HealthcareProfessional, at a ClinicLocation and time that works, and wants to know before leaving the desk or hanging up that the booking is final.
- **Healthcare Professional:** wants Appointments only in the TimeSlots that were made available for them, never two Patients in one TimeSlot.
- **Clinic:** wants no double bookings, no Patient booked twice at overlapping times, and an ActivityRecord of every booking.

# Preconditions

- The Administrative Staff member is identified by the system.
- Availability exists: TimeSlots were made (Make Availability, SO25) or imported (Import CSV, SO31).

# Success guarantee (postconditions)

- An Appointment is recorded for the Patient in the chosen TimeSlot, with status BOOKED. Through the TimeSlot it is linked to the HealthcareProfessional and the ClinicLocation. There is no Requested status: the booking is final when it is recorded.
- The chosen TimeSlot is used up: its status is BOOKED and Search Availability no longer returns it.
- An ActivityRecord of the booking is saved and concerns the Patient.
- The Administrative Staff member has the Appointment details to give to the Patient.

# Main success scenario

1. A Patient asks the Administrative Staff for an appointment, at the front desk or by phone. The Administrative Staff enters the Patient's phone number or patient number to start the booking.
2. The System identifies the Patient and shows the Patient's name and patient number.
3. The Administrative Staff searches for open TimeSlots by HealthcareProfessional, MedSpecialty and/or date range (includes **Search Availability**). The System presents the open TimeSlots that match.
4. The Administrative Staff agrees on one TimeSlot with the Patient and selects it.
5. The System verifies that the TimeSlot is still open and that the Patient has no other Appointment at an overlapping time.
6. The System records an Appointment for the Patient in that TimeSlot with status BOOKED and marks the TimeSlot as BOOKED.
7. The System saves an ActivityRecord of the booking.
8. The System presents the Appointment details (Patient, HealthcareProfessional, ClinicLocation, date and time). The Administrative Staff gives them to the Patient.

# Extensions

**1a.** No Patient has this phone number or patient number (new Patient):

> 1. The Administrative Staff registers the Patient (NC3 Register Patient, SO18). The System gives the new Patient a patient number automatically. A RAMQ number is optional because some Patients have none.
> 2. Resume at step 2 with the new Patient.

**2a.** Several Patients share the phone number (for example, members of one family):

> 1. The System lists the matching Patients with their patient numbers.
> 2. The Administrative Staff asks the Patient which one is theirs. Resume at step 1 with that patient number.

**3a.** No open TimeSlot matches the search:

> 1. The Administrative Staff changes the search (another HealthcareProfessional, MedSpecialty or dates) and repeats step 3, or
> 2. At the Patient's request, the Administrative Staff places the Patient on the waiting list (extension point: **W1 Add Patient to Waitlist**, SO27). Use case ends with no Appointment recorded.

**4a.** The service needs resources (a room, equipment or a nurse role):

> 1. The Administrative Staff inspects and selects resources for the TimeSlot (extension point: **R1 Inspect / Select Resources**, SO33 and SO34). Resume at step 5. Analysis only in Iteration II; the design is deferred to Iteration III as the handout suggests.

**5a.** The TimeSlot was booked by someone else after it was presented:

> 1. The System tells the Administrative Staff that the TimeSlot is no longer available. Nothing is recorded.
> 2. Resume at step 3.

**5b.** The Patient already has an Appointment that overlaps the TimeSlot:

> 1. The System shows the conflicting Appointment. Nothing is recorded.
> 2. Resume at step 3.

**\*a.** At any time before step 6, the Patient changes their mind or the call drops, and the Administrative Staff cancels the booking:

> 1. The System discards the unfinished booking. Use case ends with no Appointment recorded and no TimeSlot changed.

# Special requirements

- Search Availability shows only open TimeSlots; BOOKED TimeSlots are never offered.
- Every booking is saved in the ActivityRecord.
- Booking must be quick enough to complete while the Patient waits at the desk or stays on the line.

# Technology and data variations

- Step 1: the Patient is found by phone number or by patient number. The RAMQ number is never required to find a Patient.
- Step 3: any of HealthcareProfessional, MedSpecialty and date range may be left empty to mean "any".

# Frequency of occurrence

Many times a day at every ClinicLocation.

# Open issues

- Referral-only MedicalServices: in the PoC a TimeSlot belongs to a HealthcareProfessional and names no MedicalService, so the referral check of Iteration 1 (old extension 2a) is not done at booking yet. To be designed in Iteration III with MedicalService.
- Resources (E4a) are specified by Part A (CO33, CO34); allocating them when the Appointment is recorded is Iteration III design work.
- Should the Administrative Staff check insurance when booking, or only at NC1 Register Arrival?

# Change log (Iteration 1 to Iteration 2)

| # | Change | Reason |
|---|--------|--------|
| 1 | Primary actor changed from Patient to Administrative Staff; the Patient is now a supporting actor at the desk or on the phone. | The PoC feature is booking for a patient who arrived in a clinic or calls (handout, Iteration II). |
| 2 | Removed the Requested status and the "staff will confirm it" step. The Appointment is recorded with status BOOKED. | Booked directly (team decision). Matches AppointmentStatus in the PoC. |
| 3 | Old steps 1 and 3 (Patient gives a MedicalService, System lists locations, professionals and open times) replaced by an include of Search Availability in step 3. | Search Availability is its own use case (SO26) and PoC feature; the use case diagram shows the include. |
| 4 | Availability is explicit TimeSlots; step 6 marks the chosen TimeSlot BOOKED. Removed "holds the time in the Schedule". | Updated domain model: TimeSlot with OPEN / BOOKED status, occupied by at most one non-cancelled Appointment. |
| 5 | New step 1 and extensions 1a and 2a: find the Patient by phone or patient number; register a new Patient. | Patients may arrive or call before they are registered; some have no RAMQ number. |
| 6 | Extension 3a now offers the waiting list (W1); new extension 4a for resources (R1). | New Iteration II requirements; same extension points as the updated use case diagram. |
| 7 | Old extension 2a (referral required) moved to Open issues. | TimeSlots carry no MedicalService in the PoC. |
| 8 | Old extension 6a (save failure) removed. | Not meaningful for the in-memory PoC; persistence is Iteration III. |
| 9 | Extensions 5a and 5b kept with the same numbers; they now resume at step 3 (Search Availability). | Same checks, new step layout. |
| 10 | System operations renamed in place: SO01 startBooking, SO02 bookTimeSlot, SO03 cancelBooking (were beginAppointmentRequest, selectAppointmentTime, cancelAppointmentRequest). The use case name is kept. | Numbers SO01 to SO03 and the name "Request Appointment" are kept so the team's numbering, use case diagram and SSDs stay consistent. |
