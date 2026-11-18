package ats.service;

import java.time.LocalDate;
 
public interface RecurrencePattern {
    
    boolean includes(LocalDate date);
    
}
