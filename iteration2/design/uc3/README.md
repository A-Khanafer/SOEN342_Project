# UC3 Issue Referral realizations

One design sequence diagram per original system operation: [SO08](so08-begin-referral.svg), [SO09](so09-issue-referral.svg), [SO10](so10-cancel-referral.svg). Contracts were read in `iteration1/SOEN342-SSDs-and-Operation-Contracts.pdf`, CO8–CO10, pp. 12–13, and checked against `iteration1/UC3-issue-referral.docx`. The system signatures are unchanged.

| Original guarantee | Realization |
| --- | --- |
| CO8: authorize user, find patient, no Referral on successful start | ReferralController checks UserSession before lookup and retains only patient/issuer context. |
| CO8: rejected start creates ActivityRecord, optional patient absent | Explicit create messages in the unauthorized and missing-patient branches; no clinical disclosure. |
| CO9: missing reason / receiver mismatch permit retry | Break fragments end the current operation, log refusal and retain SO08 context. They return to UC3 M3. |
| CO9: offered specialty, no open duplicate | ClinicRecords checks specialty plus practice location; Patient finds its existing open referral. A matching specialty alone is insufficient. |
| CO9: successful Referral, issue date and clinical reason; issuer/patient/specialty/optional receiver links | Patient creates Referral from supplied values. A success ActivityRecord and the Referral are registered together. No external issuer or Appointment is introduced. |
| CO9: save failure leaves no new recorded referral/links | Detached objects are discarded on failed recording; earlier registered audits remain. |
| CO10: discard unfinished input, retain existing business/audit state | Controller clears context only; no Referral is marked cancelled or deleted. |

GRASP Controller assigns these system events to a small use-case controller. Information Expert assigns patient-specific duplicate detection to Patient and clinic-wide specialty/practice information to ClinicRecords. Creator assigns Referral construction to Patient, which owns the patient association and receives the other required data. ClinicRecords owns in-memory referral/audit collections and coordinates their registration. This collection holder is a design object, not an added conceptual domain service.

Visibility follows lecture 11: controller dependencies (`session`, `records`) are attributes; the current patient and issuer become attributes after SO08; the selected specialty and receiver enter as parameters; lookup results and newly created objects have local visibility; constructor parameters initialize domain associations. No global variables are needed. Create messages start new lifelines at their creation height; alt/opt/break fragments describe mutually exclusive paths and optional input.

These are proposed design responsibilities for Part C's consolidated design class diagram, **not Java classes implemented by Part A**:

| Design class | Responsibilities / messages to consolidate |
| --- | --- |
| ReferralController | The three existing system signatures; retain/clear unfinished patient and issuer context; delegate validation and recording. |
| UserSession | `getActor()`, `canIssueReferral()`, `getProfessional()`; retain the identified actor/role. |
| ClinicRecords | `findPatient`, `isSpecialtyOffered`, `recordActivity`, `makeActivity`, `recordReferralAndActivity`; retain patients, referrals and audit collections. |
| Patient | `findOpenReferral`, `makeReferral`, `addReferral`; extend the existing Part B Patient rather than replacing it. |
| HealthcareProfessional | Reuse Part B's `hasSpecialty`; clinic practice-location data must be supplied by the consolidated model. |
| Referral / ActivityRecord | Initialize the attributes/associations in CO9 and CO8. No appointment booking responsibility. |

`recordReferralAndActivity` means one logical registration action; it does not prescribe a database transaction. The diagrams show its failure guarantee without implementing a persistence layer. Full referral/audit classes, patient referral collections, role checks and professional practice-location links do not yet exist in Part B. Part C must reconcile these names with its controller/collection naming before coding UC3; it should not duplicate ProfessionalRegistry or Patient.

ClinicRecords retains the shared ProfessionalRegistry and the clinic practice-location associations as attributes to answer `isSpecialtyOffered`. It must not infer that a professional practises at a clinic merely from a name match. The identified actor used in SO08 audits is obtained from UserSession; SO09 uses the retained issuing professional.

Original unresolved rules are preserved: referral expiry, the meaning of “open”, initial priority/status in CO9, and auditing an E6a recording failure. These diagrams adopt CO9's explicit no-new-recorded-state failure guarantee and retain earlier audits. They do not invent referral status defaults or make Issue Referral an extension of Conduct Consultation.
