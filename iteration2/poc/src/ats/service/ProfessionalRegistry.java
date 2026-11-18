package ats.service;

import ats.domain.HealthcareProfessional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
new java file, keeps exactly one HealthcareProfessional object per doctor, so every part of the
system works with the same instance.
The sample CSV has no licence number, so professionals are matched by name
Two different doctors with the same name would be merged.
*/

public class ProfessionalRegistry {

    private final Map<String, HealthcareProfessional> byName = new HashMap<>();

    // Returns the professional with this name, creating and storing them the first time. 
    public HealthcareProfessional findOrCreate(String title, String firstName, String lastName,
                                               String professionType) {
        String key = key(firstName, lastName);
        HealthcareProfessional exists = byName.get(key);
        if (exists != null){
            return exists;
        }
        HealthcareProfessional created = new HealthcareProfessional(title, firstName, lastName, null, professionType);
        byName.put(key, created);
        return created;
    }

    //The professional with this name, or null if none is registered. 
    public HealthcareProfessional find(String firstName, String lastName) {
        return byName.get(key(firstName, lastName));
    }

    public List<HealthcareProfessional> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(byName.values()));
    }

    private String key(String firstName, String lastName) {
        if (firstName == null || lastName == null) {
            throw new IllegalArgumentException("First and last name are required");
        }
        return (firstName.trim() + " " + lastName.trim()).toLowerCase();
    }
}
