package ats.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HealthcareProfessional {
    
    private final String title;
    private final String firstName;
    private final String lastName;
    private final String licenseNumber;
    private final String professionType;
    private static int nextId = 1;
    private final int id;
    
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
        this.id = nextId;
        nextId++;

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
    public int getID(){
        return id;
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

    //override equals()
    @Override 
    public boolean equals(Object o){
        
        if (o == this){
            return true;
        }
        else if (!(o instanceof HealthcareProfessional)){
            return false;

        }
        HealthcareProfessional other = (HealthcareProfessional) o;
        return id == other.id;
    }

    //override hashCode

    @Override
    public int hashCode(){
        return Integer.hashCode(id);
    }



}
