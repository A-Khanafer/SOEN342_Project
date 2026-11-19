package ats.service;

import ats.domain.ActivityRecord;
import ats.domain.Patient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps every ActivityRecord (Pure Fabrication; Creator of ActivityRecord).
 * Same class and parameter order as the UC2 design: record(user, action, patient).
 */
public class ActivityLog {
    private final List<ActivityRecord> records = new ArrayList<>();

    public ActivityRecord record(String performedBy, String action, Patient patient) {
        ActivityRecord rec = new ActivityRecord(LocalDateTime.now(), action, performedBy, patient);
        records.add(rec);
        return rec;
    }

    public List<ActivityRecord> getAll() {
        return Collections.unmodifiableList(records);
    }
}
