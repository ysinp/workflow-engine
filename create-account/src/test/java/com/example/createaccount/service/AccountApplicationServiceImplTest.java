package com.example.createaccount.service;

import com.example.createaccount.dto.ApplicationStatusResponse;
import com.example.createaccount.dto.SubmitApplicationRequest;
import com.example.createaccount.entity.Account;
import com.example.createaccount.entity.AccountApplication;
import com.example.createaccount.exception.ApplicationNotFoundException;
import com.example.createaccount.exception.DuplicateEmailException;
import com.example.createaccount.exception.InvalidApplicationStateException;
import com.example.createaccount.repository.AccountApplicationRepository;
import com.example.createaccount.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a REAL BCryptPasswordEncoder and a REAL RandomDemoBalanceGenerator (only the repositories
 * are mocked), so the hashing and account-activation behavior under test is production behavior.
 */
@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceImplTest {

    private static final String PLAINTEXT = "correct-horse-battery";

    @Mock
    private AccountApplicationRepository applicationRepository;

    @Mock
    private AccountRepository accountRepository;

    private PasswordEncoder passwordEncoder;
    private AccountApplicationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AccountApplicationServiceImpl(
                applicationRepository, accountRepository, passwordEncoder, new RandomDemoBalanceGenerator());
    }

    private static SubmitApplicationRequest sampleRequest() {
        return new SubmitApplicationRequest(
                "Jane Doe", "jane@example.com", PLAINTEXT,
                null, "US", null, null, null, null, null, null, null,
                "PASSPORT", "A1234567", "Engineer", "50K_TO_100K", "EMPLOYMENT");
    }

    private static AccountApplication pendingApplication(UUID id, String passwordHash) {
        AccountApplication application = new AccountApplication();
        application.setId(id);
        application.setFullName("Jane Doe");
        application.setEmail("jane@example.com");
        application.setPasswordHash(passwordHash);
        application.setStatus(AccountApplication.PENDING_REVIEW);
        application.setCreatedAt(Instant.now());
        return application;
    }

    // ---------------------------------------------------------------- submit

    @Test
    void submit_hashesPasswordAndStagesAsPendingReview_neverTouchingAccountsTable() {
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(applicationRepository.existsByEmailAndStatus("jane@example.com", AccountApplication.PENDING_REVIEW))
                .thenReturn(false);
        when(applicationRepository.save(any(AccountApplication.class))).thenAnswer(inv -> {
            AccountApplication toSave = inv.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        ApplicationStatusResponse response = service.submit(sampleRequest());

        assertThat(response.status()).isEqualTo(AccountApplication.PENDING_REVIEW);
        assertThat(response.accountId()).isNull();
        verify(accountRepository, never()).save(any(Account.class));

        ArgumentCaptor<AccountApplication> captor = ArgumentCaptor.forClass(AccountApplication.class);
        verify(applicationRepository).save(captor.capture());
        AccountApplication saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PLAINTEXT).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PLAINTEXT, saved.getPasswordHash())).isTrue();
    }

    @Test
    void submit_withEmailAlreadyAnAccount_throwsDuplicateEmailException() {
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.submit(sampleRequest())).isInstanceOf(DuplicateEmailException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void submit_withEmailAlreadyPending_throwsDuplicateEmailException() {
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(applicationRepository.existsByEmailAndStatus("jane@example.com", AccountApplication.PENDING_REVIEW))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(sampleRequest())).isInstanceOf(DuplicateEmailException.class);
        verify(applicationRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- approve

    @Test
    void approve_createsAccountFromApplication_copyingTheAlreadyHashedPassword() {
        UUID applicationId = UUID.randomUUID();
        String hash = passwordEncoder.encode(PLAINTEXT);
        AccountApplication application = pendingApplication(applicationId, hash);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account toSave = inv.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        ApplicationStatusResponse response = service.approve(applicationId);

        assertThat(response.status()).isEqualTo(AccountApplication.APPROVED);
        assertThat(response.accountId()).isNotNull();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo("jane@example.com");
        assertThat(created.getPasswordHash()).isEqualTo(hash); // reused verbatim, not re-hashed
        assertThat(created.getRole()).isEqualTo("CUSTOMER");
        assertThat(created.getBalance()).isNotNull();
        assertThat(application.getStatus()).isEqualTo(AccountApplication.APPROVED);
    }

    @Test
    void approve_calledAgainOnAlreadyApprovedApplication_isIdempotent_doesNotCreateASecondAccount() {
        UUID applicationId = UUID.randomUUID();
        UUID existingAccountId = UUID.randomUUID();
        AccountApplication application = pendingApplication(applicationId, "hash");
        application.setStatus(AccountApplication.APPROVED);
        application.setAccountId(existingAccountId);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationStatusResponse response = service.approve(applicationId);

        assertThat(response.status()).isEqualTo(AccountApplication.APPROVED);
        assertThat(response.accountId()).isEqualTo(existingAccountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void approve_onAlreadyRejectedApplication_throwsInvalidState() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = pendingApplication(applicationId, "hash");
        application.setStatus(AccountApplication.REJECTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approve(applicationId))
                .isInstanceOf(InvalidApplicationStateException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void approve_unknownApplication_throwsNotFound() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(applicationId))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    // ---------------------------------------------------------------- reject

    @Test
    void reject_marksApplicationRejected_neverCreatingAnAccount() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = pendingApplication(applicationId, "hash");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationStatusResponse response = service.reject(applicationId);

        assertThat(response.status()).isEqualTo(AccountApplication.REJECTED);
        assertThat(response.accountId()).isNull();
        assertThat(application.getStatus()).isEqualTo(AccountApplication.REJECTED);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void reject_calledAgainOnAlreadyRejectedApplication_isIdempotent() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = pendingApplication(applicationId, "hash");
        application.setStatus(AccountApplication.REJECTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationStatusResponse response = service.reject(applicationId);

        assertThat(response.status()).isEqualTo(AccountApplication.REJECTED);
    }

    @Test
    void reject_onAlreadyApprovedApplication_throwsInvalidState() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = pendingApplication(applicationId, "hash");
        application.setStatus(AccountApplication.APPROVED);
        application.setAccountId(UUID.randomUUID());
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.reject(applicationId))
                .isInstanceOf(InvalidApplicationStateException.class);
    }

    // ---------------------------------------------------------------- getStatus

    @Test
    void getStatus_unknownApplication_throwsNotFound() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(applicationId))
                .isInstanceOf(ApplicationNotFoundException.class);
    }
}
