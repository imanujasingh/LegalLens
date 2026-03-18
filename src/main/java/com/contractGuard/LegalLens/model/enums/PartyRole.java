package com.contractGuard.LegalLens.model.enums;


import lombok.Getter;

@Getter
public enum PartyRole {
    BUYER("Buyer/Customer"),
    SELLER("Seller/Vendor"),
    EMPLOYER("Employer"),
    EMPLOYEE("Employee"),
    LICENSOR("Licensor"),
    LICENSEE("Licensee"),
    LANDLORD("Landlord"),
    TENANT("Tenant"),
    CLIENT("Client"),
    VENDOR("Vendor"),
    CUSTOMER("Customer"),
    SUPPLIER("Supplier");

    private final String displayName;

    PartyRole(String displayName) {
        this.displayName = displayName;
    }

    public static PartyRole fromString(String value) {
        try {
            return PartyRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to BUYER if unknown
            return BUYER;
        }
    }
}