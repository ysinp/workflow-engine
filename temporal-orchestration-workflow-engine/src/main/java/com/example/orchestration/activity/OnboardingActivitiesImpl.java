package com.example.orchestration.activity;

import com.example.orchestration.client.IdentificationServiceClient;
import com.example.orchestration.client.KycServiceClient;
import com.example.orchestration.workflow.FinancialData;
import com.example.orchestration.workflow.FinancialVerificationOutcome;
import com.example.orchestration.workflow.IdentificationData;
import com.example.orchestration.workflow.PersonalContactData;
import com.example.orchestration.workflow.WizardStepResult;
import feign.FeignException;
import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** All I/O for the onboarding wizard workflow lives here - identification-service and kyc-service are
 *  never called directly by the browser, only from this activity implementation. */
@Component
public class OnboardingActivitiesImpl implements OnboardingActivities {

    private static final Logger log = LoggerFactory.getLogger(OnboardingActivitiesImpl.class);

    static final String IDENTIFICATION_SERVICE_ERROR = "IdentificationServiceError";
    static final String KYC_SERVICE_ERROR = "KycServiceError";

    private final IdentificationServiceClient identificationServiceClient;
    private final KycServiceClient kycServiceClient;

    public OnboardingActivitiesImpl(IdentificationServiceClient identificationServiceClient,
                                     KycServiceClient kycServiceClient) {
        this.identificationServiceClient = identificationServiceClient;
        this.kycServiceClient = kycServiceClient;
    }

    @Override
    public WizardStepResult savePersonalContact(Long identificationId, PersonalContactData data) {
        try {
            IdentificationServiceClient.DraftResponse response = identificationServiceClient.save(
                    new IdentificationServiceClient.SaveIdentificationRequest(
                            identificationId, data.fullName(), data.dateOfBirth(), data.nationality(),
                            data.gender(), data.email(), data.phone(), data.addressLine1(), data.addressLine2(),
                            data.city(), data.state(), data.postalCode(), data.country()));
            log.info("Saved personal/contact details for identification {}", response.identificationId());
            return new WizardStepResult(response.identificationId(), response.nextStep());
        } catch (FeignException e) {
            throw serviceFailure("POST /api/identification", IDENTIFICATION_SERVICE_ERROR, e);
        }
    }

    @Override
    public WizardStepResult updateIdentification(Long identificationId, IdentificationData data) {
        try {
            IdentificationServiceClient.DraftResponse response = identificationServiceClient.updateIdentification(
                    identificationId,
                    new IdentificationServiceClient.UpdateIdentificationRequest(data.idType(), data.idNumber()));
            return new WizardStepResult(response.identificationId(), response.nextStep());
        } catch (FeignException e) {
            throw serviceFailure("PATCH /api/identification/" + identificationId, IDENTIFICATION_SERVICE_ERROR, e);
        }
    }

    @Override
    public FinancialVerificationOutcome verifyFinancial(Long identificationId, FinancialData data) {
        try {
            KycServiceClient.FinancialVerificationResponse response = kycServiceClient.verify(
                    new KycServiceClient.FinancialVerificationRequest(
                            identificationId, data.occupation(), data.annualIncome(), data.sourceOfFunds()));
            log.info("Financial verification for identification {}: verified={}", identificationId, response.verified());
            return new FinancialVerificationOutcome(response.identificationId(), response.verified(),
                    response.reasons(), response.nextStep());
        } catch (FeignException e) {
            throw serviceFailure("POST /api/kyc/verify", KYC_SERVICE_ERROR, e);
        }
    }

    private static ApplicationFailure serviceFailure(String call, String type, FeignException e) {
        return ApplicationFailure.newFailureWithCause(
                "service call " + call + " failed with HTTP " + e.status() + ": " + e.contentUTF8(),
                type,
                e);
    }
}
