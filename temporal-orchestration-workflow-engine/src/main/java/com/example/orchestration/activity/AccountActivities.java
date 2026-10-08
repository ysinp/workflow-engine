package com.example.orchestration.activity;

import com.example.orchestration.workflow.AccountCreationInput;
import com.example.orchestration.workflow.StageApplicationData;
import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface AccountActivities {
    boolean checkSubmission(String email);
    /** Stages the application in create-account (password BCrypt-hashed there); returns its applicationId. */
    String stageApplication(StageApplicationData data);
    RiskAssessment evaluateRisk(AccountCreationInput input);
    void activateAccount(String applicationId);
    void rejectApplication(String applicationId);
    void notifyApproved(String email, String fullName);
    void notifyRejected(String email, String fullName, List<String> reasons);
}
