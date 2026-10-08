package com.example.orchestration.activity;

import com.example.orchestration.workflow.FinancialData;
import com.example.orchestration.workflow.FinancialVerificationOutcome;
import com.example.orchestration.workflow.IdentificationData;
import com.example.orchestration.workflow.PersonalContactData;
import com.example.orchestration.workflow.WizardStepResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OnboardingActivities {

    /** @param identificationId null on the first submission, non-null when re-submitting after going back */
    WizardStepResult savePersonalContact(Long identificationId, PersonalContactData data);

    WizardStepResult updateIdentification(Long identificationId, IdentificationData data);

    FinancialVerificationOutcome verifyFinancial(Long identificationId, FinancialData data);
}
