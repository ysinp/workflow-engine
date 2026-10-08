package com.example.createaccount.repository;

import com.example.createaccount.entity.AccountApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, UUID> {

    boolean existsByEmailAndStatus(String email, String status);
}
