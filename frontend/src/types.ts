// ---------------------------------------------------------------------------
// KYC enums (wire values are fixed by the backend contracts)
// ---------------------------------------------------------------------------

export type IdType = 'PASSPORT' | 'NATIONAL_ID' | 'DRIVERS_LICENSE';

export type AnnualIncome =
  | 'UNDER_25K'
  | '25K_TO_50K'
  | '50K_TO_100K'
  | '100K_TO_250K'
  | 'OVER_250K';

export type SourceOfFunds = 'EMPLOYMENT' | 'BUSINESS' | 'INVESTMENTS' | 'INHERITANCE' | 'OTHER';

// ---------------------------------------------------------------------------
// Accounts service (ACCOUNTS_API_BASE)
// ---------------------------------------------------------------------------

/**
 * KYC profile fields shared by the signup request and the risk-assessment
 * submission request. Everything except `password`.
 */
export interface KycProfile {
  fullName: string;
  email: string;
  dateOfBirth: string; // ISO date, e.g. "2000-01-01"
  nationality: string; // ISO 3166-1 alpha-2
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string; // ISO 3166-1 alpha-2
  idType: IdType;
  idNumber: string;
  occupation: string;
  annualIncome: AnnualIncome;
  sourceOfFunds: SourceOfFunds;
}

// ---------------------------------------------------------------------------
// product-service (PRODUCT_API_BASE) - product-selection screen
// ---------------------------------------------------------------------------

export type ProductCategory = 'PERSONAL' | 'BUSINESS';

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  category: ProductCategory;
}

// ---------------------------------------------------------------------------
// temporal-orchestration-workflow-engine (ORCHESTRATION_API_BASE) - onboarding wizard
// ---------------------------------------------------------------------------
// The entire sign-up journey, Product Selection through Security, is one Temporal workflow,
// started the moment the product-selection screen loads. Every step is an Update call against
// that same running execution - the browser never calls identification-service, kyc-service,
// create-account or risk-score directly; those are invoked as Activities from inside the
// workflow (see OnboardingController). A HIGH risk verdict at the Security step pauses that same
// execution for a compliance reviewer rather than starting a separate workflow.

/**
 * POST {ORCHESTRATION_API_BASE}/api/orchestration/onboarding - starts the workflow.
 * `nextActivity` is the step this execution is now parked on - a genuine, named Temporal activity
 * (visible in Temporal UI's Pending Activities) that only completes when a human acts; see
 * SignUp.tsx STEP_SLUGS for how it maps to a URL. `activityId` is Temporal's own auto-assigned ID
 * for that same gate activity - purely informational (e.g. to cross-reference Temporal UI), null
 * only if that gate hadn't been scheduled yet by the time this responded.
 */
export interface StartOnboardingResponse {
  workflowId: string;
  nextActivity: string;
  activityId: string | null;
}

/** POST .../onboarding/{workflowId}/product - the product-selection screen's "Get started" button. */
export interface ProductSelectionResponse {
  workflowId: string;
  nextActivity: string;
  activityId: string | null;
}

/** POST .../onboarding/{workflowId}/personal - the wizard's Personal + Contact step. */
export interface OnboardingPersonalContactRequest {
  fullName: string;
  dateOfBirth: string;
  nationality: string;
  gender: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

/** POST .../onboarding/{workflowId}/identification - the wizard's own Identification step. */
export interface OnboardingIdentificationRequest {
  idType: IdType;
  idNumber: string;
}

/** Shared response shape for the personal and identification steps. */
export interface WizardStepResponse {
  workflowId: string;
  identificationId: number;
  nextActivity: string;
  activityId: string | null;
}

/** POST .../onboarding/{workflowId}/financial - verified against the identification already on file. */
export interface OnboardingFinancialRequest {
  occupation: string;
  annualIncome: AnnualIncome;
  sourceOfFunds: SourceOfFunds;
}

export interface FinancialVerificationResponse {
  workflowId: string;
  identificationId: number;
  verified: boolean;
  reasons: string[];
  /** nextActivity and activityId are both absent when verified is false - the wizard stays on this step. */
  nextActivity: string | null;
  activityId: string | null;
}

/**
 * POST .../onboarding/{workflowId}/security - the wizard's final step. Carries only the password;
 * every other field needed to stage the application and evaluate risk was already collected by
 * the earlier steps and is held in the workflow's own state.
 */
export interface OnboardingSecurityRequest {
  password: string;
}

/**
 * Response to the Security step. Never blocks on a reviewer: a LOW risk verdict auto-approves and
 * `result` is present immediately; a HIGH risk verdict returns with `awaitingReview=true`,
 * `nextActivity="review"` and no `result` - the frontend then polls {@link getOnboardingStatus}
 * for the compliance reviewer's eventual decision.
 */
export interface AccountCreationStepResponse {
  workflowId: string;
  riskLevel: RiskLevel;
  score: number;
  reasons: string[];
  awaitingReview: boolean;
  result?: RiskAssessmentResult;
  nextActivity?: string;
  activityId?: string;
}

export type AccountRole = 'CUSTOMER' | 'REVIEWER';

/** Account as returned by signup (201), signin (200) and the accounts list. */
export interface Account {
  id: string;
  fullName: string;
  email: string;
  createdAt: string;
  nationality: string;
  occupation: string;
  annualIncome: AnnualIncome;
  idNumberMasked: string;
  role: AccountRole;
  /** Demo-only starting balance assigned at signup; not present on REVIEWER accounts. */
  balance?: number;
}

/** POST {ACCOUNTS_API_BASE}/api/accounts/signin */
export interface SignInRequest {
  email: string;
  password: string;
}

export interface SignInResponse {
  token: string;
  account: Account;
}

// ---------------------------------------------------------------------------
// temporal-orchestration-workflow-engine (ORCHESTRATION_API_BASE) - execution status & review
// ---------------------------------------------------------------------------

export type SubmissionStatus = 'RUNNING' | 'AWAITING_REVIEW' | 'COMPLETED' | 'FAILED';

export type RiskOutcome = 'APPROVED' | 'REJECTED';

export type RiskLevel = 'LOW' | 'HIGH';

export interface RiskAssessmentResult {
  outcome: RiskOutcome;
  riskLevel: RiskLevel;
  score: number;
  reasons: string[];
}

/** GET {ORCHESTRATION_API_BASE}/api/orchestration/executions/{workflowId} */
export interface SubmissionStatusResponse {
  workflowId: string;
  status: SubmissionStatus;
  result?: RiskAssessmentResult;
  errorMessage?: string;
  /** Present only while status is AWAITING_REVIEW - the context a reviewer decides against. */
  pendingRiskAssessment?: { riskLevel: RiskLevel; score: number; reasons: string[] };
}

/** POST {ORCHESTRATION_API_BASE}/api/orchestration/executions/{workflowId}/decision - requires a REVIEWER JWT. */
export interface ReviewDecisionRequest {
  decision: 'APPROVE' | 'REJECT';
  reviewerId: string;
  notes?: string;
}

/** GET {ORCHESTRATION_API_BASE}/api/orchestration/executions/awaiting-review - requires a REVIEWER JWT. */
export interface PendingReviewItem {
  workflowId: string;
  pendingRiskAssessment: { riskLevel: RiskLevel; score: number; reasons: string[] };
}

// ---------------------------------------------------------------------------
// Orchestration engine (ORCHESTRATION_API_BASE) - saved Workflow Editor diagrams
// ---------------------------------------------------------------------------

/**
 * POST/GET {ORCHESTRATION_API_BASE}/api/workflow-definitions - `definition` is the Workflow
 * Editor's own exported diagram JSON, stored and returned verbatim (untyped from this app's
 * point of view; see WorkflowEditor.tsx for what it actually contains).
 */
export interface WorkflowDefinitionResponse {
  id: number;
  name: string;
  definition: unknown;
  updatedAt: string;
  updatedBy?: string;
}

// ---------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------

export type ApiErrorCode = 'DUPLICATE_EMAIL' | 'INVALID_CREDENTIALS' | (string & {});

export interface ApiErrorBody {
  code?: ApiErrorCode;
  message?: string;
  [key: string]: unknown;
}
