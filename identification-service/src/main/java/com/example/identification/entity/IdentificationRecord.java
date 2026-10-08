package com.example.identification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The sign-up wizard's Personal + Contact step, persisted step by step rather than held only in
 * the browser, with the Identification document attached by a later, separate step. Carries no
 * password and no final Account row; those belong to create-account once the risk-gated workflow
 * actually approves the application (see account-creation-submission). kyc-service reads this
 * record (over HTTP, not a shared database) when it verifies the Financial step.
 */
@Entity
@Table(name = "identification_records")
public class IdentificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal
    private String fullName;
    private String dateOfBirth;
    private String nationality;
    private String gender;

    // Contact
    @Column(nullable = false)
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Identification - attached by a later, separate step (see updateIdentification); null until then
    private String idType;
    private String idNumber;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected IdentificationRecord() {
        // JPA
    }

    public IdentificationRecord(String fullName, String dateOfBirth, String nationality, String gender,
                                 String email, String phone, String addressLine1, String addressLine2,
                                 String city, String state, String postalCode, String country) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Re-applies the Personal + Contact fields in place - the wizard's "Back" + re-edit +
     * "Continue" path for that step. Never touches idType/idNumber; those are only ever set by
     * {@link #updateIdentification}.
     */
    public void updatePersonalContact(String fullName, String dateOfBirth, String nationality, String gender,
                                       String email, String phone, String addressLine1, String addressLine2,
                                       String city, String state, String postalCode, String country) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.updatedAt = Instant.now();
    }

    /** Partial update (PATCH) attaching the Identification document to an existing record. */
    public void updateIdentification(String idType, String idNumber) {
        this.idType = idType;
        this.idNumber = idNumber;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public String getGender() {
        return gender;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public String getIdType() {
        return idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
