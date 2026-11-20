# Design class diagram (partial design model)

[design-class-diagram.svg](design-class-diagram.svg) (source: `design-class-diagram.mmd`, PNG export alongside). It was derived from the 10 interaction diagrams SO01-SO10 (plus SO18, used by UC1 E1a), then checked against the PoC code in `iteration2/poc/src`. Stereotypes mark who owns a class and which classes are design only (not yet Java). Arrows show navigability; every association has multiplicities at both ends.

## Where each class comes from

| Class | Interaction diagrams that use it | In PoC code |
|-------|----------------------------------|-------------|
| BookingController | SO01, SO02, SO03, SO18 | yes (Part C) |
| PatientRegistry | SO01, SO18 | yes (Part C) |
| AppointmentRegistry | SO02, SO04 | yes (Part C); `find(appointmentId)` is UC2 design only |
| ActivityLog | SO02, SO18, SO04, SO06 | yes (Part C) |
| ActivityRecord | SO02, SO04, SO06, SO08, SO09 | yes (Part C) |
| Patient | SO01, SO02, SO18, SO04, SO08, SO09 | yes (Part B) |
| Insurance, InsuranceType | SO18 | yes (Part B) |
| TimeSlot, SlotStatus | SO02, SO04 | yes (Part B) |
| Appointment, AppointmentStatus | SO02, SO04, SO06 | yes (Part B) |
| HealthcareProfessional | SO04, SO08, SO09 | yes (Part B) |
| MedSpecialty | SO09 | yes (Part B) |
| ConsultationController, UserAccount, ConsultationNotes, ClinicalRecord, Consultation | SO04-SO07 | design only (UC2) |
| ReferralController, UserSession, ClinicRecords, Referral | SO08-SO10 | design only (UC3) |
| ClinicLocation | reached through TimeSlot in SO02 and SO04 | yes (Part B) |
| AvailabilityService, SearchCriteria | SO26 searchAvailability (included in UC1 at M3); SO25 | yes (Part B) |
| ProfessionalRegistry | SO09 (ClinicRecords keeps it); SO25 | yes (Part B) |
| RecurrencePattern, DailyPattern, WeeklyPattern, MonthlyPattern | SO25 createAvailability parameter | yes (Part B) |
| CsvImportExport, ImportResult, WaitlistPatientInfo | SO31, SO32 (Part A SSDs) | yes (Part A) |
| ConsoleMenu | UI that sends every system operation (actor side of all SSDs) | yes (Part C) |

The last five rows have no design sequence diagram in Iteration II (Part B and Part A delivered SSDs and code for SO25, SO26, SO31 and SO32). They are kept because the handout asks for a partial design model covering the PoC, and the SSD operations they realize are part of the iteration.

## Message to operation check

Every message in the UC1, UC2 and UC3 diagrams is an operation of the receiving class in the diagram (for example `findByKey`, `bookTimeSlot`, `findOverlapping`, `book`, `isOpen`, `record`, `canStart`, `isAssignedTo`, `complete`, `getClinicalRecord`, `recordConsultation`, `hasClinicalAccess`, `getActor`, `canIssueReferral`, `findPatient`, `isSpecialtyOffered`, `recordActivity`, `makeActivity`, `recordReferralAndActivity`, `findOpenReferral`, `makeReferral`, `addReferral`, `hasSpecialty`). For classes that exist in Java, names, parameter types and return types match the code. Mismatches found during the check are listed in [../cross-review.md](../cross-review.md).
