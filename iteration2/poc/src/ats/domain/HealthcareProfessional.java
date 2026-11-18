package ats.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class HealthcareProfessional {
    
    private final String title;
    private final String firstName;
    private final String lastName;
    private final String licenseNumber;
    private final String professionType;
    
    private final List<MedSpecialty> specialties = new ArrayList<>();

    public HealthcareProfessional(String title, String firstName, String lastName, String licenseNumber, String professionType){

        if (firstName == null || firstName.isBlank()){
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.isBlank()){
            throw new IllegalArgumentException("Last name is required");
        } 

        this.title = title;
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.licenseNumber = licenseNumber;
        this.professionType = professionType;

    }


    public String getfName(){
        return firstName;
    }
    public String getlName(){
        return lastName;
    }
    public String getTitle(){
        return title;
    }
    public String getLNumber(){
        return licenseNumber;
    }
    public String getPType(){
        return professionType;
    }


    public void addSpecialty(MedSpecialty specialty){

       if (specialty != null && !specialties.contains(specialty)) {
        specialties.add(specialty);
        }
    }

    public boolean hasSpecialty(MedSpecialty specialty){

       return specialties.contains(specialty);

    }

    public String getFullName(){

        return firstName + " " + lastName;

    }

    public List<MedSpecialty> getSpecialties() {
    
        return Collections.unmodifiableList(specialties);
    
    }

    @Override
    public String toString(){
        if (title == null || title.isBlank()){
            return getFullName();
        }
        else {
            return title + " " + getFullName();
        }
    }



}
