package com.example.createaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A staged KYC signup awaiting a risk decision. Nothing here is a real, sign-in-able account -
 * it becomes one (a row in {@link Account}) only when a reviewer (or an auto-approved LOW-risk
 * verdict) moves it to {@link #APPROVED}. A {@link #REJECTED} application never produces an
 * {@link Account} row at all.
 */
@Entity
@Table(name = "account_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountApplication {

    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    /** BCrypt hash computed at submission time; copied verbatim onto the Account row on approval. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "status", nullable = false)
    private String status = PENDING_REVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Set only once this application is APPROVED and its Account row exists. */
    @Column(name = "account_id")
    private UUID accountId;

    // ---- KYC fields, copied onto the Account row on approval ----

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "id_type")
    private String idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "annual_income")
    private String annualIncome;

    @Column(name = "source_of_funds")
    private String sourceOfFunds;
}
