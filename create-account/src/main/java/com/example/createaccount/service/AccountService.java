package com.example.createaccount.service;

import com.example.createaccount.dto.AccountResponse;
import com.example.createaccount.dto.CreateAccountRequest;
import com.example.createaccount.dto.SignInRequest;
import com.example.createaccount.dto.SignInResponse;

import java.util.List;
import java.util.UUID;

/**
 * Business-logic contract for account management. The controller depends
 * only on this interface, never on the repository or entity directly.
 */
public interface AccountService {

    /**
     * Creates a new account (KYC signup). The plaintext password in the request is BCrypt-hashed
     * before being persisted.
     *
     * @throws com.example.createaccount.exception.DuplicateEmailException if the email is already taken
     */
    AccountResponse createAccount(CreateAccountRequest request);

    /**
     * Verifies credentials and issues a JWT.
     *
     * @throws com.example.createaccount.exception.InvalidCredentialsException if the email is unknown
     *         OR the password does not match (deliberately indistinguishable)
     */
    SignInResponse signIn(SignInRequest request);

    List<AccountResponse> getAllAccounts();

    /**
     * @throws com.example.createaccount.exception.AccountNotFoundException if no account exists with the given id
     */
    AccountResponse getAccount(UUID id);

    boolean emailExists(String email);
}
