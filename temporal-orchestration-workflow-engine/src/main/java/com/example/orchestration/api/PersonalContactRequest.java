package com.example.orchestration.api;

import com.example.orchestration.workflow.PersonalContactData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonalContactRequest(
        @NotBlank String fullName,
        @NotBlank String dateOfBirth,
        @NotBlank String nationality,
        String gender,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        String state,
        String postalCode,
        @NotBlank String country) {

    public PersonalContactData toWorkflowData() {
        return new PersonalContactData(fullName, dateOfBirth, nationality, gender, email, phone,
                addressLine1, addressLine2, city, state, postalCode, country);
    }
}
