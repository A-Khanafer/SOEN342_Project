package ats.service;

import ats.domain.HealthcareProfessional;
import ats.domain.MedSpecialty;
import java.time.LocalDate;


public class SearchCriteria {

    private final HealthcareProfessional professional;
    private final MedSpecialty specialty;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public SearchCriteria(HealthcareProfessional professional, MedSpecialty specialty,
                          LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("'To' date must not be before 'from' date");
        }
        this.professional = professional;
        this.specialty = specialty;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public static SearchCriteria any() {
        return new SearchCriteria(null, null, null, null);
    }

    public HealthcareProfessional getProfessional() {
        return professional;
    }

    public MedSpecialty getSpecialty() {
        return specialty;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}