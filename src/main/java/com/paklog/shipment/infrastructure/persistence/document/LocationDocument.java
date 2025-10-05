package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.Location;

public class LocationDocument {
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String country;

    public static LocationDocument fromDomain(Location domain) {
        if (domain == null) return null;
        LocationDocument doc = new LocationDocument();
        doc.setName(domain.name());
        doc.setAddressLine1(domain.addressLine1());
        doc.setAddressLine2(domain.addressLine2());
        doc.setCity(domain.city());
        doc.setStateOrProvince(domain.stateOrProvince());
        doc.setPostalCode(domain.postalCode());
        doc.setCountry(domain.country());
        return doc;
    }

    public Location toDomain() {
        return new Location(name, addressLine1, addressLine2, city, stateOrProvince, postalCode, country);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStateOrProvince() { return stateOrProvince; }
    public void setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
