package com.example.kyc.service;

import com.example.kyc.dto.FinancialVerificationRequest;
import com.example.kyc.dto.FinancialVerificationResponse;
import com.example.kyc.dto.IdentificationView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialVerificationServiceImplTest {

    @Mock
    private IdentificationClient identificationClient;

    private FinancialVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinancialVerificationServiceImpl(identificationClient, new KycVerificationService());
    }

    @Test
    void verify_withWellFormedIdentificationAndKnownSource_returnsVerifiedWithNextStep() {
        when(identificationClient.get(5L))
                .thenReturn(new IdentificationView(5L, "Jane Doe", "jane@example.com", "PASSPORT", "P1234567"));

        FinancialVerificationResponse response = service.verify(
                new FinancialVerificationRequest(5L, "Engineer", "UNDER_25K", "EMPLOYMENT"));

        assertThat(response.verified()).isTrue();
        assertThat(response.reasons()).isEmpty();
        assertThat(response.nextStep()).isEqualTo("security");
        assertThat(response.identificationId()).isEqualTo(5L);
    }

    @Test
    void verify_withUnrecognizedIdType_returnsNotVerifiedWithNoNextStep() {
        when(identificationClient.get(6L))
                .thenReturn(new IdentificationView(6L, "Jane Doe", "jane@example.com", "SOMETHING_ELSE", "P1234567"));

        FinancialVerificationResponse response = service.verify(
                new FinancialVerificationRequest(6L, "Engineer", "UNDER_25K", "EMPLOYMENT"));

        assertThat(response.verified()).isFalse();
        assertThat(response.reasons()).contains("Identification document type is not recognized");
        assertThat(response.nextStep()).isNull();
    }

    @Test
    void verify_withMalformedIdNumber_returnsNotVerified() {
        when(identificationClient.get(7L))
                .thenReturn(new IdentificationView(7L, "Jane Doe", "jane@example.com", "PASSPORT", "ABC"));

        FinancialVerificationResponse response = service.verify(
                new FinancialVerificationRequest(7L, "Engineer", "UNDER_25K", "EMPLOYMENT"));

        assertThat(response.verified()).isFalse();
        assertThat(response.reasons()).contains("Identification document number does not look well-formed");
    }

    @Test
    void verify_withUnrecognizedSourceOfFunds_returnsNotVerified() {
        when(identificationClient.get(8L))
                .thenReturn(new IdentificationView(8L, "Jane Doe", "jane@example.com", "PASSPORT", "P1234567"));

        FinancialVerificationResponse response = service.verify(
                new FinancialVerificationRequest(8L, "Engineer", "UNDER_25K", "LOTTERY_WINNINGS"));

        assertThat(response.verified()).isFalse();
        assertThat(response.reasons()).contains("Source of funds is not recognized");
    }
}
