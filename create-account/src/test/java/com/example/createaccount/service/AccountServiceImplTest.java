package com.example.createaccount.service;

import com.example.createaccount.config.JwtService;
import com.example.createaccount.dto.AccountResponse;
import com.example.createaccount.dto.CreateAccountRequest;
import com.example.createaccount.dto.SignInRequest;
import com.example.createaccount.dto.SignInResponse;
import com.example.createaccount.entity.Account;
import com.example.createaccount.exception.AccountNotFoundException;
import com.example.createaccount.exception.DuplicateEmailException;
import com.example.createaccount.exception.InvalidCredentialsException;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a REAL BCryptPasswordEncoder and a REAL JwtService (only the repository is mocked), so the
 * hashing and sign-in behavior under test is the production behavior.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final String TEST_SECRET = "unit-test-secret-that-is-comfortably-longer-than-32-bytes";
    private static final String PLAINTEXT = "correct-horse-battery";

    @Mock
    private AccountRepository accountRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private DemoBalanceGenerator demoBalanceGenerator;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(TEST_SECRET, 4);
        demoBalanceGenerator = new RandomDemoBalanceGenerator();
        accountService = new AccountServiceImpl(accountRepository, passwordEncoder, jwtService, demoBalanceGenerator);
    }

    private static CreateAccountRequest fullRequest(String email) {
        return new CreateAccountRequest(
                "Jane Doe", email, PLAINTEXT,
                LocalDate.of(1990, 5, 17), "US", "+1-555-0100",
                "1 Main St", "Apt 2", "Springfield", "IL", "62701", "US",
                "PASSPORT", "A1234567", "Software Engineer", "50K_TO_100K", "EMPLOYMENT");
    }

    private Account storedAccount(String email, String plaintext) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setFullName("Jane Doe");
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(plaintext));
        account.setCreatedAt(Instant.now());
        account.setNationality("US");
        account.setOccupation("Software Engineer");
        account.setAnnualIncome("50K_TO_100K");
        account.setIdNumber("A1234567");
        return account;
    }

    // ---------------------------------------------------------------- signup

    @Test
    void createAccount_withNewEmail_hashesPasswordWithBCrypt_copiesKycFields_andReturnsMinimalView() {
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        AccountResponse response = accountService.createAccount(fullRequest("jane@example.com"));

        assertThat(response.id()).isNotNull();
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.nationality()).isEqualTo("US");
        assertThat(response.occupation()).isEqualTo("Software Engineer");
        assertThat(response.annualIncome()).isEqualTo("50K_TO_100K");
        assertThat(response.idNumberMasked()).isEqualTo("****4567");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.balance()).isNotNull();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo("CUSTOMER");
        assertThat(saved.getBalance()).isNotNull();

        // Stored value is a BCrypt hash of the plaintext, never the plaintext itself.
        assertThat(saved.getPasswordHash()).isNotEqualTo(PLAINTEXT).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PLAINTEXT, saved.getPasswordHash())).isTrue();

        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 17));
        assertThat(saved.getNationality()).isEqualTo("US");
        assertThat(saved.getPhone()).isEqualTo("+1-555-0100");
        assertThat(saved.getAddressLine1()).isEqualTo("1 Main St");
        assertThat(saved.getAddressLine2()).isEqualTo("Apt 2");
        assertThat(saved.getCity()).isEqualTo("Springfield");
        assertThat(saved.getState()).isEqualTo("IL");
        assertThat(saved.getPostalCode()).isEqualTo("62701");
        assertThat(saved.getCountry()).isEqualTo("US");
        assertThat(saved.getIdType()).isEqualTo("PASSPORT");
        assertThat(saved.getIdNumber()).isEqualTo("A1234567");
        assertThat(saved.getOccupation()).isEqualTo("Software Engineer");
        assertThat(saved.getAnnualIncome()).isEqualTo("50K_TO_100K");
        assertThat(saved.getSourceOfFunds()).isEqualTo("EMPLOYMENT");
    }

    @Test
    void createAccount_withOnlyRequiredFields_succeedsWithNullKycFields() {
        CreateAccountRequest minimal = new CreateAccountRequest("Jane Doe", "jane@example.com", PLAINTEXT,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.createAccount(minimal);

        assertThat(response.nationality()).isNull();
        assertThat(response.idNumberMasked()).isEqualTo("****");
    }

    @Test
    void createAccount_withDuplicateEmail_throwsDuplicateEmailException() {
        when(accountRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(fullRequest("jane@example.com")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    // ---------------------------------------------------------------- sign-in

    @Test
    void signIn_withCorrectPassword_returnsVerifiableTokenAndAccountView() {
        when(accountRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(storedAccount("jane@example.com", PLAINTEXT)));

        SignInResponse response = accountService.signIn(new SignInRequest("jane@example.com", PLAINTEXT));

        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.parseSubject(response.token())).isEqualTo("jane@example.com");
        assertThat(jwtService.parseRole(response.token())).isEqualTo("CUSTOMER");
        assertThat(response.account().email()).isEqualTo("jane@example.com");
        assertThat(response.account().idNumberMasked()).isEqualTo("****4567");
    }

    @Test
    void signIn_withWrongPassword_throwsGenericInvalidCredentials() {
        when(accountRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(storedAccount("jane@example.com", PLAINTEXT)));

        assertThatThrownBy(() -> accountService.signIn(new SignInRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void signIn_withUnknownEmail_throwsTheSameGenericInvalidCredentials() {
        when(accountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.signIn(new SignInRequest("nobody@example.com", PLAINTEXT)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void signIn_wrongPasswordAndUnknownEmail_areIndistinguishable() {
        when(accountRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(storedAccount("jane@example.com", PLAINTEXT)));
        when(accountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> accountService.signIn(new SignInRequest("jane@example.com", "wrong-password")));
        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> accountService.signIn(new SignInRequest("nobody@example.com", PLAINTEXT)));

        assertThat(wrongPassword.getClass()).isEqualTo(unknownEmail.getClass());
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
    }

    // ---------------------------------------------------------------- reads

    @Test
    void getAccount_withMissingId_throwsAccountNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(missingId))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void emailExists_delegatesToRepository() {
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);

        assertThat(accountService.emailExists("jane@example.com")).isTrue();
    }
}
