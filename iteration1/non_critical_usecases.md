# Non-Critical Use Case Scenarios (Brief Format)

## Register Arrival

On the day of the appointment, the patient checks in at the front desk. The Administrative Staff finds the patient in the system (this step includes Search Patient). The system shows the patient’s appointments for today, and the staff member selects the one the patient is here for. The system records the arrival time on that appointment, marks it as arrived, and logs an activity record.


## Manage Appointment

The Administrative Staff finds an existing appointment for a patient and chooses to confirm, reschedule, or cancel it. To reschedule, the staff member picks a new date and time. The system checks the healthcare professional’s availability and scheduling constraints at that clinic location, then updates the appointment. To confirm or cancel, the system simply updates the appointment’s status. In every case, the system saves the change and logs an activity record.


## Register Patient

The Administrative Staff enters a new patient’s personal information: first name, last name, date of birth, and phone. If the patient has insurance, the staff member also enters it (RAMQ or private, with number and provider). The system checks that the patient isn’t already registered. It then creates the patient with a unique patient number, creates an empty clinical record for them, and logs an activity record


## Search Patient

A user (Administrative Staff or Healthcare Professional) searches for a patient. They can search by insurance number, date of birth, phone number, or a partial first and/or last name. The system shows the matching patients, and the user selects one. What the user can see about that patient depends on their role: administrative staff see only administrative details


## Process Uninsured Payment

The Administrative Staff selects an appointment for an uninsured service. The system gets the fee for that medical service and generates an invoice. The staff member records the payment amount and method. The system registers the payment against the invoice, updates the invoice status (paid or partially paid), and logs an activity record.


## Record External Referral

A patient arrives with a referral from a physician who doesn’t practise at the clinic. The Administrative Staff finds the patient and enters the external physician’s name, licence number, and contact. They also enter the referral details: issue date, clinical reason, priority, and the specialty or professional being requested. The system saves the referral with status “pending” so it can be used to book appointments, and logs an activity record.


## View Clinical Record

The Healthcare Professional finds the patient and asks to see their clinical record. The system checks that the user’s role allows access to clinical information. It then shows the record’s consultations, including observations, diagnoses, treatment recommendations, and prescriptions. The system logs who opened the record and when.


## Issue Prescription (extends Conduct Consultation)

During a consultation, the Healthcare Professional decides a prescription is needed. For each medication, they select it and enter the dosage, frequency, and duration, then add any general instructions. The system creates the prescription with its issue date, links it to the consultation, and logs an activity record. Electronic transmission to pharmacies is outside the system’s scope.


## Request Follow-up Appointment (extends Conduct Consultation)

During a consultation, the Healthcare Professional decides the patient needs a follow-up and specifies the service and time frame. The system finds open times in the professional’s schedule. A follow-up appointment is booked, linked to that consultation, and logged.

