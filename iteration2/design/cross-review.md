# Cross-review: diagrams against code (Part C, Nov 19, 2026)

Every interaction diagram (UC1 SO01-SO03 and SO18, UC2 SO04-SO07, UC3 SO08-SO10) was checked against the PoC code in `iteration2/poc/src` and against the consolidated class diagram. Mismatches found and what was done:

| # | Where | Finding | Resolution |
|---|-------|---------|------------|
| 1 | UC2 diagrams, UC1 code | UC2 calls `ActivityLog.record(user, action, patient)`. | Part C implemented `ActivityLog.record(performedBy, action, patient)` with the same parameter order; UC2 passes the user's name. |
| 2 | UC2 SO04, code | `AppointmentRegistry.find(appointmentId)` needs an appointment id, but `Appointment` has none. | Kept in the class diagram as a UC2 design operation; implementing it needs an id on Appointment (Iteration III, when UC2 is coded). |
| 3 | Part B `TimeSlot.overlaps`, UC1 E5b | `overlaps` only compares slots of the same professional, so it cannot detect a patient booked with two professionals at once. | `AppointmentRegistry.findOverlapping` compares date and times directly (same rule as Part A's CSV importer). SO02 diagram shows `timesOverlap`. |
| 4 | Part A `CsvImportExport`, menu | The importer needs the SAME patient and appointment lists as the booking code. | `ConsoleMenu` creates the lists once and passes them to `PatientRegistry`, `AppointmentRegistry` and `CsvImportExport`. Tested by `CsvBookingIntegrationTest`. |
| 5 | Part A scenarios, UC1 | Part A refers to UC1 M3 (open slots), M5 (checks), M6 (slot occupied) and E3a (waitlist). | Revised UC1 uses exactly these step numbers. |
| 6 | UC3 `ClinicRecords`, UC1/UC2 | `ClinicRecords` keeps patients and audits, overlapping `PatientRegistry` and `ActivityLog`. | Both kept in the class diagram as designed; merging them is noted for Iteration III when UC3 is coded. |
| 7 | UC2 `UserAccount`, UC3 `UserSession` | Two design classes for the identified user. | Both kept as designed; to be merged in Iteration III (UC2 README already notes this). |
| 8 | `Appointment.cancel()` | Only BOOKED appointments can be cancelled (throws otherwise). | The menu lists only BOOKED appointments for cancellation. |
| 9 | `ats.App` | `App.main` is Part B's availability self-check, not a menu. | The menu entry point is `ats.ui.ConsoleMenu`; `App` is left unchanged (README lists both). |
| 10 | UC1 SO02, Part B | The diagram's `ts.book()` exists as package-private `TimeSlot.book()`, called only by the `Appointment` constructor. | Matches; noted in the UC1 README. |

No other mismatches: every message in the UC1 diagrams is a method of the receiving class in the code and in the class diagram.
