# UC2 Conduct Consultation realizations

One design sequence diagram per original system operation: [SO04](so04-start-consultation.svg), [SO05](so05-enter-consultation-notes.svg), [SO06](so06-end-consultation.svg), [SO07](so07-abandon-consultation.svg). Contracts are CO4–CO7 in `iteration1/SOEN342-SSDs-and-Operation-Contracts.pdf`. The system signatures are unchanged.

| Original guarantee | Realization |
| --- | --- |
| CO4: appointment confirmed, for today, assigned to this professional; clinical access authorized | Appointment answers `canStart(today)` (status BOOKED or ARRIVED, slot date = today) and `isAssignedTo(h)` using its TimeSlot; UserAccount answers `hasClinicalAccess()`. |
| CO4: refusals E2b/E3a are audited; E2a records nothing | Separate `alt` operands; only the E2b/E3a operand calls `ActivityLog.record(...)`. No refusal shows the ClinicalRecord. |
| CO4: opening the ClinicalRecord is audited | Patient returns its ClinicalRecord; ActivityLog creates the ActivityRecord. |
| CO5: notes are unsaved input; no domain change | Controller creates a non-persistent ConsultationNotes; a repeated submission replaces it. |
| CO6: Consultation created, linked to Appointment and ClinicalRecord; Appointment completed; audited | ClinicalRecord creates and adds the Consultation; Appointment completes itself and links it; ActivityLog creates the ActivityRecord. |
| CO7: discard unsaved notes; no domain change | Controller destroys ConsultationNotes and clears its context; Appointment status and earlier audits are kept. |

GRASP Controller assigns the four system events to ConsultationController, a use-case controller that keeps the consultation context between operations. Information Expert assigns eligibility and completion to Appointment and the ClinicalRecord lookup to Patient. Creator assigns Consultation creation to ClinicalRecord (it contains Consultations) and ActivityRecord creation to ActivityLog. AppointmentRegistry, ActivityLog and ConsultationNotes are Pure Fabrications.

Design classes for the consolidated class diagram (UC2 is design-only in Iteration II; these are not yet Java classes):

| Design class | Responsibilities / messages |
| --- | --- |
| ConsultationController | `startConsultation`, `enterConsultationNotes`, `endConsultation`, `abandonConsultation`; keeps appointment, ClinicalRecord, user and notes between operations. |
| AppointmentRegistry | `find(appointmentId)` |
| UserAccount | `getProfessional()`, `hasClinicalAccess()`; the identified user's role and linked HealthcareProfessional. |
| ActivityLog | `record(user, action, patient)`; creates ActivityRecords. |
| ConsultationNotes | Holds reasonForVisit, findings and diagnosis until the consultation ends. |
| Appointment | Add `canStart(today)`, `isAssignedTo(h)`, `complete(c)`, `getPatient()` to the existing class. |
| Patient | Add `getClinicalRecord()`. |
| ClinicalRecord | `recordConsultation(a, notes)`, `add(c)`. |

Decisions on Iteration I open points: `canStart` accepts ARRIVED as well as BOOKED (CO4 noted that Register Arrival would otherwise block UC2); a repeated SO05 replaces earlier notes (CO5 left append vs. replace open); E7a save failure applies once persistence exists (Iteration III). Class names may change if the team aligns them with UC3 (`UserSession`, `ClinicRecords`).
