package com.example.createaccount.service;

import com.example.createaccount.dto.ApplicationStatusResponse;
import com.example.createaccount.dto.SubmitApplicationRequest;

import java.util.UUID;

/**
 * Staging area for a KYC signup awaiting a risk decision. No {@code Account} row (and therefore
 * no sign-in capability) exists until {@link #approve} runs; {@link #reject} never creates one.
 */
public interface AccountApplicationService {

    /**
     * Stages a new application (password BCrypt-hashed immediately, never stored in plaintext).
     *
     * @throws com.example.createaccount.exception.DuplicateEmailException if this email already
     *         has an account, or a pending application
     */
    ApplicationStatusResponse submit(SubmitApplicationRequest request);

    /**
     * @throws com.example.createaccount.exception.ApplicationNotFoundException if no application
     *         exists with the given id
     */
    ApplicationStatusResponse getStatus(UUID applicationId);

    /**
     * Creates the real Account row from the staged application. Idempotent: calling this again
     * on an already-APPROVED application returns the same result without creating a second
     * account (Temporal activities execute at-least-once).
     *
     * @throws com.example.createaccount.exception.ApplicationNotFoundException  if no application
     *         exists with the given id
     * @throws com.example.createaccount.exception.InvalidApplicationStateException if the
     *         application was already REJECTED
     */
    ApplicationStatusResponse approve(UUID applicationId);

    /**
     * Marks the application rejected. No Account row is ever created for it. Idempotent: calling
     * this again on an already-REJECTED application returns the same result.
     *
     * @throws com.example.createaccount.exception.ApplicationNotFoundException  if no application
     *         exists with the given id
     * @throws com.example.createaccount.exception.InvalidApplicationStateException if the
     *         application was already APPROVED
     */
    ApplicationStatusResponse reject(UUID applicationId);
}
