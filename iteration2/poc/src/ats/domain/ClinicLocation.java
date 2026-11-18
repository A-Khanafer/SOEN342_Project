package ats.domain;

public class ClinicLocation {
    
    private final String name;
    private final String address;
    private final String phone; 


    public ClinicLocation(String name, String address, String phone){


        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Clinic location name is required");
        }

        this.name = name.trim();
        this.address = address;
        this.phone = phone;
    }

    public String getName(){
        return name;
    }
    public String getAddress(){
        return address;
    }
    public String getPhone(){
        return phone;
    }

    @Override
    public String toString(){
        return name;
    }

}
