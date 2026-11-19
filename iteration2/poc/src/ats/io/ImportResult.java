package ats.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Row outcomes; a rejected data row does not prevent later rows from being imported. */
public final class ImportResult {
    int rowsImported;
    int rowsSkipped;
    int slotsImported;
    int appointmentsImported;
    int patientsImported;
    int waitlistInfoImported;
    final List<String> errors = new ArrayList<>();

    public int getRowsImported() { return rowsImported; }
    public int getRowsSkipped() { return rowsSkipped; }
    public int getSlotsImported() { return slotsImported; }
    public int getAppointmentsImported() { return appointmentsImported; }
    public int getPatientsImported() { return patientsImported; }
    public int getWaitlistInfoImported() { return waitlistInfoImported; }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    @Override
    public String toString() {
        return "imported=" + rowsImported + ", skipped=" + rowsSkipped + ", rejected=" + errors.size()
                + ", slots=" + slotsImported + ", appointments=" + appointmentsImported
                + ", new patients=" + patientsImported + ", waitlist information=" + waitlistInfoImported;
    }
}
