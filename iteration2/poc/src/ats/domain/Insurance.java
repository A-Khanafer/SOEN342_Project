package ats.domain;


public class Insurance {

    private final InsuranceType type;
    private final String number;
    private final String provider;

    public Insurance(InsuranceType type, String number, String provider) {
        if (type == null) {
            throw new IllegalArgumentException("Insurance type is required");
        }
        if (type == InsuranceType.RAMQ && (number == null || number.isBlank())) {
            throw new IllegalArgumentException("A RAMQ number is required");
        }
        if (type == InsuranceType.PRIVATE && (provider == null || provider.isBlank())) {
            throw new IllegalArgumentException("A private insurance provider is required");
        }
        this.type = type;
        this.number = number == null ? null : number.trim();
        this.provider = provider == null ? null : provider.trim();
    }

    public InsuranceType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return type == InsuranceType.RAMQ ? "RAMQ " + number : provider;
    }
}