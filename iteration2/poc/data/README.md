# CSV demonstration data and format

The three CSVs transcribe the assignment's sample tables (pp. 4–5), with the visual waitlist row numbers omitted. They are demonstration inputs, not automated test files. Accented names are UTF-8. The sample dates are retained even when the demonstration runs later; historical availability can be imported.

Import in this order:

1. `professional-availability.csv`: eight slots for five professionals.
2. `booked-appointments.csv`: four appointments, including Fatima Benali without RAMQ.
3. `waitlist-patients.csv`: five patient-information records, including Maya Nguyen with private insurance and no RAMQ, and Alexandre Dubois with both RAMQ and private insurance.

## Headers and values

Headers are case-sensitive after trimming; column order is flexible. Missing, duplicate and unknown headers are file errors. Extra application fields are accepted only when listed below. Quoted commas/newlines and doubled quotes are supported, with LF or CRLF and an optional UTF-8 BOM. Structural CSV errors are detected before processing rows. Field values are trimmed.

| Kind / method | Required headers | Optional headers |
| --- | --- | --- |
| AVAILABILITY / `importAvailability` | `First Name,Last Name,Title,Specialty,Date,Start,End` | `Location,Profession Type,Status,Patient,Patient First Name,Patient Last Name,Telephone,RAMQ No.,Insurance,Insurance No.` |
| APPOINTMENTS / `importAppointments` | `Professional,Date,Start,End,Patient,Telephone,RAMQ No.` | `Location,Insurance,Insurance No.,Patient First Name,Patient Last Name` |
| WAITLIST_PATIENTS / `importWaitlistPatients` | `First Name,Last Name,Telephone,RAMQ,Insurance,Referral Type,Description` | `No.,Insurance No.` |

Names, dates, start/end times and patient telephone are required where applicable; referral type/description are required in waitlist information. Title/Specialty headers are required but values may be empty if unknown. Dates use valid `yyyy-MM-dd`; times use valid `HH:mm`, optionally ISO seconds/fraction. End must follow start on the same day. Patient telephone must contain digits. Empty, `-`, `–` and `—` mean absent insurance. RAMQ is not required. A private insurance number is optional; supplying a number without its provider is rejected.

Missing Location uses the ClinicLocation passed to the importer. A named imported location has unknown address/phone. The sample has no licence numbers or profession type; the existing registry uses names, and absent profession type falls back to Title. Multiple professional specialties are separated by `;` in the Specialty field (that separator is reserved in specialty names).

The sample appointment Patient field splits at the first whitespace: first token is the first name and the remainder the surname. Supply both optional `Patient First Name` and `Patient Last Name` to represent compound first names unambiguously; exports always provide them. RAMQ takes precedence for patient reuse. Without it, normalized first/last names and telephone digits are matched. Ambiguous identity/conflicting RAMQ is rejected. Existing immutable name/phone/date-of-birth fields are retained; the source files contain no birth dates. This heuristic cannot replace a permanent patient identifier.

## Occupancy and reusable exports

Each sample availability row is one OPEN TimeSlot, not a working window to subdivide. Imported appointment professional/date/start/end and optional location must match a shared slot exactly. Creation uses Part B's `new Appointment(patient, slot)`, which changes occupancy to BOOKED. Conflicting bookings and patient overlaps are rejected. Already-imported bookings for the same patient/slot are skipped; duplicate availability slots and staged information are also skipped. Valid rows remain imported when other data rows fail.

Exports contain the required availability columns plus all optional availability columns listed above. Every slot is exported using `AvailabilityService.getAllSlots()`. BOOKED rows include patient identity so `importAvailability(exportPath)` can reconstruct occupancy in fresh memory through Appointment creation. Importing the assignment's appointments afterward safely skips matching bookings. OPEN rows never contain booking data. An explicit OPEN row cannot reopen an occupied slot; cancellation must use Appointment behavior. If Status is omitted, importing an existing availability row preserves its current occupancy.

Export requires a **new** file path with an existing writable parent. No overwrite prompt is built into Part A. An empty dataset exports headers. The shared appointment list must include every occupied slot's appointment. Export refuses inconsistent occupancy, missing booked-patient telephone or more than one private insurance for a booked patient, rather than emitting a snapshot that cannot be restored with this format.

This is an availability/occupancy snapshot. It does not back up clinical records, cancelled-appointment history, arrival/completion lifecycle, waitlist information, location contact data, licences, resources or audits. Restored occupied slots have BOOKED Appointments. Optional insurance input is retained in Patient (zero or more insurance records); the export's flat insurance extension represents RAMQ plus at most one private insurer per booked patient.

## Waitlist information limits

The sample has no medical service, explicit requested professional/specialty/date range, initial priority, request date or verified referral issuer/issue date. `WaitlistPatientInfo` retains its actual Patient plus referral type and description for Part C to display and complete. It is an import result object, not an eligible domain WaitlistEntry or a valid clinical Referral. Do not infer a specialty from free-text descriptions. The optional No. column is ignored and is not the system patient number.
