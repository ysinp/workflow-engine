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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** BCrypt hash computed by this service; the plaintext password is never stored. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // role/balance are intentionally nullable at the DB level (not just in the Java default) so
    // that Hibernate's ddl-auto:update can add these columns to an already-populated accounts
    // table without a migration; every code path that reads them treats null the same as the
    // documented default (see AccountMapper). New rows always have both set explicitly.

    /**
     * "CUSTOMER" | "REVIEWER". Never settable by the public signup endpoint - always CUSTOMER
     * there. REVIEWER accounts exist only via {@code config.ReviewerAccountSeeder}, matching this
     * POC's single-reviewer-persona design (see account-creation-submission's ReviewerAuthGuard,
     * which trusts this claim from the JWT to gate the compliance decision endpoint).
     */
    @Column(name = "role")
    private String role = "CUSTOMER";

    /**
     * Demo-only account balance, assigned once at account creation by
     * {@code service.DemoBalanceGenerator} (no real funding happens in this POC).
     */
    @Column(name = "balance", precision = 14, scale = 2)
    private BigDecimal balance;

    // ---- KYC fields (all nullable; demo-grade, deliberately not over-constrained) ----

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** ISO 3166-1 alpha-2, e.g. "US", "IR". */
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

    /** ISO 3166-1 alpha-2. */
    @Column(name = "country")
    private String country;

    /** "PASSPORT" | "NATIONAL_ID" | "DRIVERS_LICENSE". */
    @Column(name = "id_type")
    private String idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "occupation")
    private String occupation;

    /** "UNDER_25K" | "25K_TO_50K" | "50K_TO_100K" | "100K_TO_250K" | "OVER_250K". */
    @Column(name = "annual_income")
    private String annualIncome;

    /** "EMPLOYMENT" | "BUSINESS" | "INVESTMENTS" | "INHERITANCE" | "OTHER". */
    @Column(name = "source_of_funds")
    private String sourceOfFunds;
}
