package com.example.createaccount.config;

import com.example.createaccount.entity.Account;
import com.example.createaccount.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Creates the single demo compliance-reviewer account this POC uses, if it does not already
 * exist. There is no reviewer signup flow - REVIEWER accounts only ever come from here, which
 * is what makes trusting the "role" claim in a JWT safe for {@code ReviewerAuthGuard} in
 * account-creation-submission.
 * <p>
 * Credentials default to demo values so the leadership demo works out of the box; override
 * REVIEWER_EMAIL/REVIEWER_PASSWORD for anything beyond a local demo.
 */
@Component
public class ReviewerAccountSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAccountSeeder.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String reviewerEmail;
    private final String reviewerPassword;

    public ReviewerAccountSeeder(AccountRepository accountRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${reviewer.seed.email}") String reviewerEmail,
                                 @Value("${reviewer.seed.password}") String reviewerPassword) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.reviewerEmail = reviewerEmail;
        this.reviewerPassword = reviewerPassword;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.existsByEmail(reviewerEmail)) {
            return;
        }
        Account reviewer = new Account();
        reviewer.setFullName("Compliance Reviewer");
        reviewer.setEmail(reviewerEmail);
        reviewer.setPasswordHash(passwordEncoder.encode(reviewerPassword));
        reviewer.setCreatedAt(Instant.now());
        reviewer.setRole("REVIEWER");
        reviewer.setBalance(BigDecimal.ZERO);
        accountRepository.save(reviewer);
        log.info("Seeded the demo compliance-reviewer account ({})", reviewerEmail);
    }
}
