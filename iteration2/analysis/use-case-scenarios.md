# Iteration II use-case additions

The three critical use cases remain UC1 Request Appointment, UC2 Conduct Consultation and UC3 Issue Referral. NC1–NC9 retain the names and behavior in `iteration1/non_critical_usecases.md`. The updated diagram is [use-case-diagram.svg](use-case-diagram.svg).

## Existing scenario changes

UC1 M3 presents explicit open slots; M5 checks the selected slot, patient overlap, applicable referral and required resources; M6 occupies that slot and allocates required resources. Administrative staff may act for an arriving or telephone patient, as allowed in Iteration I and the Iteration II PoC. UC1 E3a now offers optional waitlist placement. No placement occurs without the patient's request. NC2 cancellation uses revised CO17; rescheduling releases old occupancy only when replacement booking succeeds. UC2/UC3 flows and the two existing consultation extensions are retained. Whether Issue Referral extends Conduct Consultation remains an original open issue, so no new extension is asserted.

## W1 Add patient to waitlist — SO27

Actor: Administrative Staff, acting on the patient's request. An identified, registered patient needs a medical service and no suitable open slot is available. Staff enters the service, any requested professional/specialty/date range, and a priority. The system checks the criteria and creates a waiting entry. Invalid patient/service/references, reversed dates, missing criteria, duplicate active entry or suitable availability produce an explanation and no new entry. If a slot is available, staff can return to UC1.

## W2 Find eligible waitlisted patients — SO28

Actor: Administrative Staff. Staff selects an open slot and medical service, often following cancellation. The system presents matching active entries, ordered by priority, or an empty list. An unknown/occupied slot or unsupported service is rejected. This query makes no offers or bookings.

## W3 Record telephone offer — SO29

Actor: Administrative Staff. From eligible candidates staff calls the patient outside the software, then records ACCEPTED or DECLINED for the selected slot/service. The system records the offer. A decline removes the entry only if staff explicitly chooses removal; otherwise the entry stays waiting. Acceptance proceeds to ordinary UC1 booking, which rechecks the slot and resources. Stale slot/entry or invalid outcome is rejected. Recording an acceptance neither reserves nor books the slot.

## W4 Change waitlist priority — SO30

Actor: Administrative Staff. Staff selects an active entry and assigns another valid priority. The system records it and subsequent candidate lists reflect the change. Unknown/inactive entries or invalid priority are rejected.

## C1 Import CSV — SO31

Actor: Administrative Staff. Staff supplies a readable source path and kind (AVAILABILITY, APPOINTMENTS or WAITLIST_PATIENTS), in that order. The system validates CSV structure and headers, then validates each data record and reports imported/skipped/rejected counts and record errors. Availability creates/reuses professionals, specialties, locations and slots. Appointments reuse matching slots and patients and occupy the slots. Waitlist input retains patient, optional insurance and referral description for staff completion. Invalid file/CSV structure rejects the file before importing rows; invalid data rejects that row while later rows continue. Exact duplicate slots/bookings/information are skipped rather than duplicated. Files can have headers only (zero records).

## C2 Export availability CSV — SO32

Actor: Administrative Staff. Staff supplies a new destination file in an existing writable directory. The system exports all availability, including BOOKED slots and their patient identity, and reports the file path and count. An invalid/existing destination or inconsistent slot/appointment data yields an error. The exported snapshot can restore slot occupancy into a fresh system through AVAILABILITY import. Existing records are not overwritten or cancelled by import.

## R1 Inspect and select resources — SO33/SO34

Actor: Administrative Staff. For a service, location and interval, staff requests available predefined resources by requirement. If the service needs resources, staff selects a satisfying set for the unfinished UC1 booking; insufficient resources prevent completion. SO02 revalidates and forms allocations together with booking. The analysis identifies these events; the detailed resource design is deferred as suggested by the assignment, p. 2.

## Explicit analysis conventions

- Supplied professional, specialty and date criteria are combined; at least one is supplied. Date bounds are inclusive and appointment intervals are `[start,end)` on one date.
- Eligibility also requires the requested service, no patient overlap and applicable referral/resource checks. Waitlist placement does not establish clinical referral validity.
- Priority is an orderable staff value. The assignment gives no numeric scale; use the team's chosen scale consistently. FIFO request time is a proposed tie-breaker, pending team agreement.
- Active means WAITING. Entries become FULFILLED only after successful ordinary booking, or REMOVED after the explicit decline/removal choice. Acceptance alone leaves WAITING and creates no hold. Equivalent active patient/service/criteria combinations are treated as duplicates.
- A predefined Resource represents one allocatable unit (including a role unit). ResourceRequirement.quantity states how many units of its kind are needed at the selected location. No assignment to specific staff is implied.
