# Iteration II system operations and contracts

Sources: assignment pp. 2–5; Iteration I operation-contract PDF pp. 7–19; scenarios [W1–W4, C1–C2, R1](use-case-scenarios.md). Lecture 7: black-box system events; lecture 8: instance, attribute and association postconditions. References identify domain concepts, not Java objects in the public UI. FilePath, CsvKind, Priority, DateRange and result summaries are value types. Preconditions describe established context; the failures below are checks, not assumptions of success.

SO01–SO24 retain their numbers. SO25/SO26 are reserved for Part B's making/searching availability; Part A starts at SO27. CO27 corresponds to SO27, etc. All new waitlist/resource operations are analysis specifications, not implemented CSV methods.

| Operation | Black-box SSD | Cross-reference |
| --- | --- | --- |
| SO27 `addPatientToWaitlist(patientNumber, medicalService, requestedProfessional?, requestedSpecialty?, dateRange?, priority)` | [SO27](ssd/so27-add-to-waitlist.svg) | W1; UC1 E3a |
| SO28 `findEligibleWaitlistedPatients(slot, medicalService)` | [SO28](ssd/so28-find-eligible.svg) | W2; NC2 cancellation |
| SO29 `recordWaitlistOfferOutcome(entry, slot, medicalService, outcome, removeIfDeclined)` | [SO29](ssd/so29-record-offer.svg) | W3 |
| SO30 `changeWaitlistPriority(entry, priority)` | [SO30](ssd/so30-change-priority.svg) | W4 |
| SO31 `importCsv(sourceFile, kind)` | [SO31](ssd/so31-import-csv.svg) | C1 |
| SO32 `exportAvailabilityCsv(destinationFile)` | [SO32](ssd/so32-export-csv.svg) | C2 |
| SO17 `cancelAppointment(appointment)` | [SO17 revised](ssd/so17-cancel-appointment.svg) | NC2; W2 |
| SO33 `findAvailableResources(medicalService, clinicLocation, date, start, end)` | [Resource SSD](ssd/so33-34-resources.svg) | R1; UC1 M3–M5 |
| SO34 `selectAppointmentResources(slot, resources: Sequence<ResourceRef>)` | [Resource SSD](ssd/so33-34-resources.svg) | R1; UC1 M4–M6 |

`?` denotes optional input. The service identifies what the patient needs; at least one target professional, specialty or date range is required. When recording an offer, medicalService must equal the entry's service. No operation places a telephone call automatically.

## Shared eligibility and occupancy rules

A candidate entry is WAITING, has the selected service, and matches every supplied professional/specialty/date criterion. Its patient has no overlapping occupied appointment and meets that service's applicable referral conditions. The selected professional provides the service and any requested specialty at the slot's location. The slot must be OPEN and enough predefined resources must be available for the whole interval. Return candidates in priority order (proposed FIFO tie-breaker). These are analysis rules; the CSV importer does not implement clinical eligibility, waiting lists or resources.

At most one non-cancelled Appointment occupies a slot. Cancelled appointments retain their slot association as history; multiple historical appointments may therefore reference a slot. OPEN means no current occupant, and BOOKED means occupied, independently of the Appointment's Requested/Confirmed/Arrived lifecycle. Resource allocations for overlapping occupied appointments may not reuse the same resource unit. See the domain-model constraints.

## CO27 — Add patient to waitlist

**Operation:** `addPatientToWaitlist(patientNumber: PatientNumber, medicalService: MedicalServiceRef, requestedProfessional: Optional<HealthcareProfessionalRef>, requestedSpecialty: Optional<MedSpecialtyRef>, dateRange: Optional<DateRange>, priority: Priority)`.

**Cross-references:** SO27; W1; UC1 E3a; `so27-add-to-waitlist.svg`.

**Preconditions:** An identified Administrative Staff member is acting on the patient's placement request.

**Success postconditions:**

1. Instances: a WaitlistEntry e and ActivityRecord ar were created. A WaitingList w was created only if the clinic did not already maintain it. No Appointment/Referral was created.
2. Attributes: e.priority was set to priority; e.requestDate to the request timestamp; e.status to WAITING; optional date bounds to the supplied values. ar.timestamp/action/performedBy recorded placement and the actor.
3. Associations: e was associated with w, the resolved Patient, MedicalService and any selected professional/specialty. ar was associated with that Patient through concerns.

**Failures:** Unknown patient/service/targets, incompatible professional/specialty/service, missing criteria, invalid date range/priority, an equivalent active entry, or suitable open availability: report the cause; no new entry or booking was recorded. Existing entry details may be returned for a duplicate. Registration and clinical referral creation are separate use cases.

## CO28 — Find eligible waitlisted patients

**Operation:** `findEligibleWaitlistedPatients(slot: TimeSlotRef, medicalService: MedicalServiceRef)`.

**Cross-references:** SO28; W2; revised CO17; `so28-find-eligible.svg`.

**Preconditions:** Identified Administrative Staff is examining an opening.

**Success postconditions:** Instances: none created/deleted. Attributes: none changed. Associations: none formed/broken. The returned ordered candidates satisfied the shared rules when evaluated; an empty result is successful. No entry, slot or resource was reserved.

**Failures:** Unknown/occupied slot or invalid/unsupported service: report the cause and leave domain state unchanged. A returned candidate is not a guarantee that the slot remains available later.

## CO29 — Record waitlist offer outcome

**Operation:** `recordWaitlistOfferOutcome(entry: WaitlistEntryRef, slot: TimeSlotRef, medicalService: MedicalServiceRef, outcome: OfferOutcome, removeIfDeclined: Boolean)`.

**Cross-references:** SO29; W3; CO28; ordinary UC1/CO2; `so29-record-offer.svg`.

**Preconditions:** Identified Administrative Staff has made a telephone offer to the entry's patient. Outcome is the staff's report of that conversation/attempt, not an automated contact.

**Success postconditions:**

1. Instances: a WaitlistOffer o and ActivityRecord ar were created. No Appointment was created.
2. Attributes: o.offeredAt was set to the recorded attempt time and o.outcome to ACCEPTED or DECLINED. e.status was set to REMOVED only for DECLINED with removeIfDeclined=true; otherwise it remained WAITING. Priority was unchanged. ar recorded time, outcome/removal choice and actor.
3. Associations: o was associated with e and the offered TimeSlot. ar was associated with e's Patient. No appointment occupancy or resource allocation association was formed/broken.

**Failures:** Invalid/stale/non-WAITING entry, unknown/occupied slot, service mismatch, failed eligibility recheck, invalid outcome, or removeIfDeclined=true with a non-decline outcome: report the cause; no offer or entry change was recorded. Recheck is a simple policy for this specification; the assignment does not define late recording of old telephone calls.

**Result:** Acceptance invites ordinary UC1 booking; it creates no hold. Booking rechecks availability/resources and, when explicitly linked to this entry, sets it FULFILLED and forms its Appointment link. A failed booking leaves the slot/entry unchanged. Decline without removal leaves the patient waiting. No automatic next offer is made.

## CO30 — Change waitlist priority

**Operation:** `changeWaitlistPriority(entry: WaitlistEntryRef, priority: Priority)`.

**Cross-references:** SO30; W4; `so30-change-priority.svg`.

**Preconditions:** Identified Administrative Staff is maintaining the waiting list.

**Success postconditions:** Instances: ActivityRecord ar was created. Attributes: the resolved WAITING entry's priority was set to the supplied valid value; its requestDate/status and criteria were unchanged; ar recorded time, priority change and actor. Associations: ar was associated with the entry's Patient; list membership and other links were retained. Future queries reflect the new ordering; existing query results need refreshing.

**Failures:** Unknown/inactive entry or invalid priority: report the cause with no entry change. Reapplying the same valid priority has no additional business effect.

## CO31 — Import CSV

**Operation:** `importCsv(sourceFile: FilePath, kind: CsvKind)` where kind is AVAILABILITY, APPOINTMENTS or WAITLIST_PATIENTS. Part C dispatches to the corresponding public Java method.

**Cross-references:** SO31; C1; assignment pp. 4–5; `so31-import-csv.svg`; `ats.io.CsvImportExport`.

**Preconditions:** Identified authorized staff selected an import kind. Shared in-memory state exists (possibly empty). APPOINTMENTS input requires matching availability to have been imported/created; absence is checked per row.

**Success/partial-success postconditions, for each accepted record:**

1. Instances: AVAILABILITY created absent HealthcareProfessionals, MedSpecialties, ClinicLocations and TimeSlots. An extended BOOKED availability record with patient identity additionally created/reused Patient/Insurance and created an Appointment. APPOINTMENTS created/reused Patient/Insurance and created an Appointment for an existing slot. WAITLIST_PATIENTS created/reused Patient/Insurance; imported referral type/description were retained as unfinished input for staff completion, not a domain Referral or WaitlistEntry.
2. Attributes: new slot date/start/end and initial OPEN status were set from validated input. Successful booked input set its slot to BOOKED through Appointment creation. New professional, patient and insurance values were set from available columns; absent birth dates/licences/contact fields remained unspecified. Existing immutable demographics were retained. No imported availability row changed an occupied slot to OPEN.
3. Associations: slots were associated with their professional/location; specialties with their professional; insurance with its patient. Booked appointments were associated with the resolved patient and slot. No clinical/referral/resource associations were inferred from descriptive CSV text.

**Failures/report:** Unreadable/non-regular path, invalid UTF-8, bad CSV structure or missing/duplicate/unknown headers: file rejected before any rows were imported. Invalid required values, impossible dates/times, overlaps, conflicting identity or unmatched/occupied slots: row rejected with record number, other valid rows retained. Exact duplicate slots/bookings/information were skipped. Returned counts distinguish imported, skipped and rejected rows, new slots, appointments, patients and staged waitlist information. Header-only files return zero counts. This is a partial import, not a whole-file transaction.

**PoC limits:** Identity is by registry name for professionals; patient RAMQ when present, otherwise normalized first/last name and telephone. Ambiguity/conflicting RAMQ is rejected. RAMQ absence is permitted. Optional private insurer/number is retained without inventing a policy number. Full-name appointment fields split at the first space unless explicit patient first/last columns are supplied; see CSV documentation. The sample cannot supply service/referral verification, ClinicalRecord creation, activity audits or resource allocations; those remain outside this importer.

## CO32 — Export availability CSV

**Operation:** `exportAvailabilityCsv(destinationFile: FilePath)`.

**Cross-references:** SO32; C2; assignment p. 4; `so32-export-csv.svg`; `ats.io.CsvImportExport.exportAvailability`.

**Preconditions:** Identified authorized staff requested an export; the shared in-memory dataset is accessible.

**Success postconditions:** Instances: no domain instances were created/deleted. Attributes: no domain attributes were changed. Associations: none formed/broken. A UTF-8 CSV file was written containing every slot from the availability dataset (including BOOKED), its professional/specialties, date/start/end, location and occupancy. Booked rows included their patient's identity/contact and representable insurance for reusable occupancy import. Result: absolute file path and slot-record count. An empty dataset produces a header-only CSV.

**Failures:** Missing/invalid destination, nonexistent/unwritable parent, existing destination, inconsistent occupied-slot/Appointment association or unrepresentable data: report an error, leaving domain state unchanged. File writing errors may leave an incomplete destination; discard it before retrying. Export represents availability and booking occupancy, not a complete backup of clinical/lifecycle/waitlist data.

## Revised CO17 — Cancel appointment

**Operation:** `cancelAppointment(appointment: AppointmentRef)` (signature unchanged).

**Cross-references:** SO17; NC2 S1b, S4–S5; assignment p. 2 cancellation/waitlist paragraph; CO28; `so17-cancel-appointment.svg`.

**Preconditions:** Identified Administrative Staff has selected cancellation of a stored appointment. Cancellability and slot ownership are checked rather than assumed.

**Success postconditions:**

1. Instances: ActivityRecord ar was created. Appointment a, its patient, slot and referrals were retained. No replacement Appointment, waitlist entry or offer was created.
2. Attributes: a.status was set to cancelled; its previously occupied TimeSlot t.status was set to OPEN. ar.timestamp/action/performedBy recorded cancellation and the actor. Waitlist priorities/statuses were unchanged.
3. Associations: ar was associated with a's Patient through concerns. a's patient, professional, service, location, referral and historical slot links were retained. Active resource-allocation links for a were broken, releasing those units for its interval.

**Result:** The returned cancellation details included t and the candidates eligible for a's medical service under CO28, evaluated after release; the list may be empty. It is a query result, not a booking/hold/contact. Staff chooses whether to call a candidate and uses SO29 plus ordinary UC1 booking if accepted.

**Failures/convention:** Unknown appointment, an already-cancelled/arrived/completed appointment, or no matching occupied slot: refuse with no additional release/booking. Requested/Confirmed appointments are cancellable in the analysis model; Part B's PoC permits BOOKED only. A repeated cancellation is rejected. The single-process PoC's existing `Appointment.cancel()` releases its slot; candidate lookup, resource release and audit orchestration belong to future integration and are not added to that class here. A future save failure must retain pre-operation cancellation/occupancy/allocation state; no database behavior is implemented.

## CO33 — Find available resources (analysis only)

**Operation:** `findAvailableResources(medicalService: MedicalServiceRef, clinicLocation: ClinicLocationRef, date: Date, start: Time, end: Time)`.

**Cross-references:** SO33; R1; UC1 M3–M5; assignment p. 2; `so33-34-resources.svg`.

**Preconditions:** Identified authorized staff is examining an appointment interval.

**Postconditions:** Instances: none. Attributes: none. Associations: none. Returned predefined resources at that location were free for the interval, grouped by the service's ResourceRequirements; unmet quantities were reported. No resource was allocated. A service with no resource requirements returns an empty requirement set successfully.

**Failures:** Unknown service/location or invalid interval: report error, no domain change.

## CO34 — Select appointment resources (analysis only)

**Operation:** `selectAppointmentResources(slot: TimeSlotRef, resources: Sequence<ResourceRef>)`.

**Cross-references:** SO34; R1; UC1 M4–M6; CO2; `so33-34-resources.svg`.

**Preconditions:** SO01 established unfinished booking context for a service and patient, before final SO02 recording. Staff supplies the proposed slot together with the resource selection. This is a proposed extension to the UC1 selection flow for resource-requiring services; no earlier slot-selection system event is assumed.

**Postconditions:** Instances: none in recorded domain state. Attributes: none in recorded domain state. Associations: none in recorded domain state. The proposed slot and chosen resources were retained only as unfinished input for SO02. Final booking must select that same slot, recheck every requirement and availability, and form the Appointment–Resource allocations together with slot occupancy; an incomplete allocation must not produce a booking.

**Failures:** Unknown/occupied/incompatible slot, missing/wrong-kind/duplicate/unavailable resources, resources outside a requirement's permitted set, wrong location or unmet quantity: report error, retain no invalid selection and form no allocations. No detailed resource design or implementation is introduced in Part A.
