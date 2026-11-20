package ats.domain;

import java.time.LocalDateTime;

/** One audit entry: who did what, when, and which Patient it concerns (domain model: ActivityRecord concerns Patient). */
public class ActivityRecord {
    private final LocalDateTime timestamp;
    private final String action;
    private final String performedBy;
    private final Patient patient;

    public ActivityRecord(LocalDateTime timestamp, String action, String performedBy, Patient patient) {
        this.timestamp = timestamp;
        this.action = action;
        this.performedBy = performedBy;
        this.patient = patient;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getPerformedBy() { return performedBy; }
    public Patient getPatient() { return patient; }

    @Override
    public String toString() {
        return timestamp.withNano(0) + " | " + performedBy + " | " + action
                + (patient == null ? "" : " | patient " + patient.getPatientNumber());
    }
}
