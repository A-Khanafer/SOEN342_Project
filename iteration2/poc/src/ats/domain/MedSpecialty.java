package ats.domain;

public class MedSpecialty {
    
    private final String name;

    public MedSpecialty(String name){
        
        //refuse bad input
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Specialty name required");
        }
        
        this.name = name.trim();
    }

    public String getName(){
        return name;
    }

    //override equals()
    @Override 
    public boolean equals(Object o){
        
        if (o == this){
            return true;
        }
        else if (!(o instanceof MedSpecialty)){
            return false;

        }
        MedSpecialty other = (MedSpecialty) o;
        return name.equalsIgnoreCase(other.name);
    }

    //override hashCode

    @Override
    public int hashCode(){
        return name.toLowerCase().hashCode();
    }
    
    //override toString()

    @Override
    public String toString(){
        return name;
    }


}
