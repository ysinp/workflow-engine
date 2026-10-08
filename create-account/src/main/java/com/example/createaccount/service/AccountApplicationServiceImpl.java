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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AccountApplicationServiceImpl implements AccountApplicationService {

    private final AccountApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoBalanceGenerator demoBalanceGenerator;

    public AccountApplicationServiceImpl(AccountApplicationRepository applicationRepository,
                                         AccountRepository accountRepository,
                                         PasswordEncoder passwordEncoder,
                                         DemoBalanceGenerator demoBalanceGenerator) {
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoBalanceGenerator = demoBalanceGenerator;
    }

    @Override
    @Transactional
    public ApplicationStatusResponse submit(SubmitApplicationRequest request) {
        boolean alreadyTaken = accountRepository.existsByEmail(request.email())
                || applicationRepository.existsByEmailAndStatus(request.email(), AccountApplication.PENDING_REVIEW);
        if (alreadyTaken) {
            throw new DuplicateEmailException(request.email());
        }

        AccountApplication application = new AccountApplication();
        application.setFullName(request.fullName());
        application.setEmail(request.email());
        application.setPasswordHash(passwordEncoder.encode(request.password()));
        application.setStatus(AccountApplication.PENDING_REVIEW);
        application.setCreatedAt(Instant.now());
        application.setDateOfBirth(request.dateOfBirth());
        application.setNationality(request.nationality());
        application.setPhone(request.phone());
        application.setAddressLine1(request.addressLine1());
        application.setAddressLine2(request.addressLine2());
        application.setCity(request.city());
        application.setState(request.state());
        application.setPostalCode(request.postalCode());
        application.setCountry(request.country());
        application.setIdType(request.idType());
        application.setIdNumber(request.idNumber());
        application.setOccupation(request.occupation());
        application.setAnnualIncome(request.annualIncome());
        application.setSourceOfFunds(request.sourceOfFunds());

        AccountApplication saved = applicationRepository.save(application);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationStatusResponse getStatus(UUID applicationId) {
        return toResponse(findOrThrow(applicationId));
    }

    @Override
    @Transactional
    public ApplicationStatusResponse approve(UUID applicationId) {
        AccountApplication application = findOrThrow(applicationId);
        if (AccountApplication.APPROVED.equals(application.getStatus())) {
            return toResponse(application); // idempotent retry
        }
        if (!AccountApplication.PENDING_REVIEW.equals(application.getStatus())) {
            throw new InvalidApplicationStateException(
                    "Cannot approve application '" + applicationId + "' - already " + application.getStatus());
        }

        Account account = new Account();
        account.setFullName(application.getFullName());
        account.setEmail(application.getEmail());
        account.setPasswordHash(application.getPasswordHash());
        account.setCreatedAt(Instant.now());
        account.setRole("CUSTOMER");
        account.setBalance(demoBalanceGenerator.generateInitialBalance());
        account.setDateOfBirth(application.getDateOfBirth());
        account.setNationality(application.getNationality());
        account.setPhone(application.getPhone());
        account.setAddressLine1(application.getAddressLine1());
        account.setAddressLine2(application.getAddressLine2());
        account.setCity(application.getCity());
        account.setState(application.getState());
        account.setPostalCode(application.getPostalCode());
        account.setCountry(application.getCountry());
        account.setIdType(application.getIdType());
        account.setIdNumber(application.getIdNumber());
        account.setOccupation(application.getOccupation());
        account.setAnnualIncome(application.getAnnualIncome());
        account.setSourceOfFunds(application.getSourceOfFunds());
        Account savedAccount = accountRepository.save(account);

        application.setStatus(AccountApplication.APPROVED);
        application.setDecidedAt(Instant.now());
        application.setAccountId(savedAccount.getId());
        applicationRepository.save(application);

        return toResponse(application);
    }

    @Override
    @Transactional
    public ApplicationStatusResponse reject(UUID applicationId) {
        AccountApplication application = findOrThrow(applicationId);
        if (AccountApplication.REJECTED.equals(application.getStatus())) {
            return toResponse(application); // idempotent retry
        }
        if (!AccountApplication.PENDING_REVIEW.equals(application.getStatus())) {
            throw new InvalidApplicationStateException(
                    "Cannot reject application '" + applicationId + "' - already " + application.getStatus());
        }

        application.setStatus(AccountApplication.REJECTED);
        application.setDecidedAt(Instant.now());
        applicationRepository.save(application);

        return toResponse(application);
    }

    private AccountApplication findOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private static ApplicationStatusResponse toResponse(AccountApplication application) {
        return new ApplicationStatusResponse(application.getId(), application.getStatus(), application.getAccountId());
    }
}
