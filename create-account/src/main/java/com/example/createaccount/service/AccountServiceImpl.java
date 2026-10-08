package com.example.createaccount.service;

import com.example.createaccount.config.JwtService;
import com.example.createaccount.dto.AccountMapper;
import com.example.createaccount.dto.AccountResponse;
import com.example.createaccount.dto.CreateAccountRequest;
import com.example.createaccount.dto.SignInRequest;
import com.example.createaccount.dto.SignInResponse;
import com.example.createaccount.entity.Account;
import com.example.createaccount.exception.AccountNotFoundException;
import com.example.createaccount.exception.DuplicateEmailException;
import com.example.createaccount.exception.InvalidCredentialsException;
import com.example.createaccount.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DemoBalanceGenerator demoBalanceGenerator;

    /**
     * Hash of a throwaway value, used to run a password comparison even when the email is unknown
     * so both sign-in failure paths take roughly the same time (mitigates user enumeration by timing).
     */
    private final String dummyPasswordHash;

    public AccountServiceImpl(AccountRepository accountRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService,
                              DemoBalanceGenerator demoBalanceGenerator) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.demoBalanceGenerator = demoBalanceGenerator;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalization-dummy-password");
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Account account = new Account();
        account.setFullName(request.fullName());
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setCreatedAt(Instant.now());
        account.setRole("CUSTOMER");
        account.setBalance(demoBalanceGenerator.generateInitialBalance());

        account.setDateOfBirth(request.dateOfBirth());
        account.setNationality(request.nationality());
        account.setPhone(request.phone());
        account.setAddressLine1(request.addressLine1());
        account.setAddressLine2(request.addressLine2());
        account.setCity(request.city());
        account.setState(request.state());
        account.setPostalCode(request.postalCode());
        account.setCountry(request.country());
        account.setIdType(request.idType());
        account.setIdNumber(request.idNumber());
        account.setOccupation(request.occupation());
        account.setAnnualIncome(request.annualIncome());
        account.setSourceOfFunds(request.sourceOfFunds());

        Account saved = accountRepository.save(account);
        return AccountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        Optional<Account> maybeAccount = accountRepository.findByEmail(request.email());

        // Always run exactly one BCrypt comparison, whether or not the account exists.
        String hashToCheck = maybeAccount.map(Account::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (maybeAccount.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        Account account = maybeAccount.get();
        String token = jwtService.generateToken(account.getEmail(), account.getRole());
        return new SignInResponse(token, AccountMapper.toResponse(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return accountRepository.existsByEmail(email);
    }
}
