package ats.io;

import ats.domain.Patient;

/** Imported information awaiting staff completion; neither a valid Referral nor a WaitlistEntry. */
public final class WaitlistPatientInfo {
    private final Patient patient;
    private final String referralType;
    private final String description;

    WaitlistPatientInfo(Patient patient, String referralType, String description) {
        this.patient = patient;
        this.referralType = referralType;
        this.description = description;
    }

    public Patient getPatient() { return patient; }
    public String getReferralType() { return referralType; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return patient + " | " + patient.getPhone() + " | insurance=" + patient.getInsurances()
                + " | " + referralType + " | " + description;
    }
}
