package com.example.submission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AccountCreationSubmissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountCreationSubmissionApplication.class, args);
    }
}
