import type {
  Account,
  AccountCreationStepResponse,
  ApiErrorBody,
  FinancialVerificationResponse,
  OnboardingFinancialRequest,
  OnboardingIdentificationRequest,
  OnboardingPersonalContactRequest,
  OnboardingSecurityRequest,
  PendingReviewItem,
  ProductCategory,
  ProductResponse,
  ProductSelectionResponse,
  ReviewDecisionRequest,
  SignInResponse,
  StartOnboardingResponse,
  SubmissionStatusResponse,
  WizardStepResponse,
  WorkflowDefinitionResponse,
} from '../types';

// Runtime config pattern: prefer window.__ENV__ (written at container start by
// docker-entrypoint.sh from ACCOUNTS_API_URL / ORCHESTRATION_API_URL), then fall
// back to the Vite build-time env var (from a local .env file), then a
// hardcoded default so `npm run dev` works out of the box.
declare global {
  interface Window {
    __ENV__?: {
      VITE_ACCOUNTS_API?: string;
      VITE_ORCHESTRATION_API?: string;
      VITE_PRODUCT_API?: string;
    };
  }
}

const ACCOUNTS_API_BASE =
  window.__ENV__?.VITE_ACCOUNTS_API ||
  import.meta.env.VITE_ACCOUNTS_API ||
  'http://localhost:8083';

const ORCHESTRATION_API_BASE =
  window.__ENV__?.VITE_ORCHESTRATION_API ||
  import.meta.env.VITE_ORCHESTRATION_API ||
  'http://localhost:8081';

const PRODUCT_API_BASE =
  window.__ENV__?.VITE_PRODUCT_API ||
  import.meta.env.VITE_PRODUCT_API ||
  'http://localhost:8087';

export class ApiError extends Error {
  status: number;
  code: string | undefined;
  body: unknown;

  constructor(message: string, status: number, body: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
    const maybeCode = (body as ApiErrorBody | null)?.code;
    this.code = typeof maybeCode === 'string' ? maybeCode : undefined;
  }
}

/**
 * Thrown specifically when an authenticated call gets a 401 (missing, invalid
 * or expired token). Extends ApiError so generic handlers still work, but lets
 * callers `instanceof`-check it to force a logout + redirect to sign-in.
 */
export class UnauthorizedError extends ApiError {
  constructor(message: string, body: unknown) {
    super(message, 401, body);
    this.name = 'UnauthorizedError';
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let body: unknown = null;
    try {
      body = await response.json();
    } catch {
      // ignore - body may not be JSON
    }
    const fromBody = (body as ApiErrorBody | null)?.message;
    const message =
      typeof fromBody === 'string' && fromBody.trim()
        ? fromBody
        : `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, body);
  }
  return (await response.json()) as T;
}

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// ---------------------------------------------------------------------------
// Accounts service
// ---------------------------------------------------------------------------

/** 200 -> { token, account }, 401 -> ApiError(code INVALID_CREDENTIALS). */
export async function signIn(email: string, password: string): Promise<SignInResponse> {
  const response = await fetch(`${ACCOUNTS_API_BASE}/api/accounts/signin`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ email, password }),
  });
  return handleResponse<SignInResponse>(response);
}

/** Requires a bearer token. Throws UnauthorizedError on 401. */
export async function fetchAccounts(token: string): Promise<Account[]> {
  const response = await fetch(`${ACCOUNTS_API_BASE}/api/accounts`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  try {
    return await handleResponse<Account[]>(response);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      throw new UnauthorizedError(err.message, err.body);
    }
    throw err;
  }
}

// ---------------------------------------------------------------------------
// temporal-orchestration-workflow-engine - onboarding wizard execution status & review
// ---------------------------------------------------------------------------
// The Security step's submitSecurityStep never blocks on a reviewer - a HIGH risk verdict pauses
// the same onboarding workflow and these three functions inspect/resolve that pause directly,
// rather than going through a separate submission service's workflow.

export async function getOnboardingStatus(workflowId: string): Promise<SubmissionStatusResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/executions/${encodeURIComponent(workflowId)}`,
  );
  return handleResponse<SubmissionStatusResponse>(response);
}

/**
 * A compliance reviewer's decision on an execution currently AWAITING_REVIEW. Requires the
 * signed-in reviewer's bearer token - the backend's ReviewerAuthGuard rejects anything else with
 * a 403.
 */
export async function submitReviewDecision(
  workflowId: string,
  decision: ReviewDecisionRequest,
  token: string,
): Promise<void> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/executions/${encodeURIComponent(workflowId)}/decision`,
    {
      method: 'POST',
      headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` },
      body: JSON.stringify(decision),
    },
  );
  if (!response.ok) {
    await handleResponse(response); // throws ApiError with the parsed message
  }
}

/** Every execution currently paused for a compliance decision. Requires a reviewer bearer token. */
export async function listPendingReviews(token: string): Promise<PendingReviewItem[]> {
  const response = await fetch(`${ORCHESTRATION_API_BASE}/api/orchestration/executions/awaiting-review`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<PendingReviewItem[]>(response);
}

// ---------------------------------------------------------------------------
// Orchestration engine - saved Workflow Editor diagrams
// ---------------------------------------------------------------------------

/**
 * Persists a Workflow Editor diagram (the SDK's own exported JSON, verbatim) so it survives
 * across browsers/reloads instead of living only in the editor's localStorage. Upserts by
 * `definition.name`. Requires a reviewer bearer token.
 */
export async function saveWorkflowDefinition(
  definition: unknown,
  token: string,
): Promise<WorkflowDefinitionResponse> {
  const name = (definition as { name?: string })?.name;
  const response = await fetch(`${ORCHESTRATION_API_BASE}/api/workflow-definitions`, {
    method: 'POST',
    headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` },
    body: JSON.stringify({ name, definition }),
  });
  return handleResponse<WorkflowDefinitionResponse>(response);
}

/** Loads a previously saved diagram by name. Requires a reviewer bearer token. */
export async function getWorkflowDefinition(
  name: string,
  token: string,
): Promise<WorkflowDefinitionResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/workflow-definitions/${encodeURIComponent(name)}`,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  return handleResponse<WorkflowDefinitionResponse>(response);
}

// ---------------------------------------------------------------------------
// product-service - the sign-up wizard's product-selection screen
// ---------------------------------------------------------------------------

/** category omitted/undefined returns the full catalog; otherwise narrows to that category. */
export async function listProducts(category?: ProductCategory): Promise<ProductResponse[]> {
  const query = category ? `?category=${encodeURIComponent(category)}` : '';
  const response = await fetch(`${PRODUCT_API_BASE}/api/products${query}`);
  return handleResponse<ProductResponse[]>(response);
}

// ---------------------------------------------------------------------------
// temporal-orchestration-workflow-engine - onboarding wizard
// ---------------------------------------------------------------------------
// The product-selection screen starts the OnboardingWizardWorkflow as soon as it loads; every
// step from there on - including the risk-gated Security step - is an Update call against that
// same running execution, which internally invokes identification-service, kyc-service,
// create-account and risk-score as Activities. The browser never talks to those services directly.

/** POST /api/orchestration/onboarding - starts the workflow. */
export async function startOnboarding(): Promise<StartOnboardingResponse> {
  const response = await fetch(`${ORCHESTRATION_API_BASE}/api/orchestration/onboarding`, {
    method: 'POST',
    headers: JSON_HEADERS,
  });
  return handleResponse<StartOnboardingResponse>(response);
}

/** Product-selection screen's "Get started" button. */
export async function submitProductSelectionStep(
  workflowId: string,
  product: string,
): Promise<ProductSelectionResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/onboarding/${encodeURIComponent(workflowId)}/product`,
    { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ product }) },
  );
  return handleResponse<ProductSelectionResponse>(response);
}

export async function submitPersonalContactStep(
  workflowId: string,
  payload: OnboardingPersonalContactRequest,
): Promise<WizardStepResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/onboarding/${encodeURIComponent(workflowId)}/personal`,
    { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(payload) },
  );
  return handleResponse<WizardStepResponse>(response);
}

export async function submitIdentificationStep(
  workflowId: string,
  payload: OnboardingIdentificationRequest,
): Promise<WizardStepResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/onboarding/${encodeURIComponent(workflowId)}/identification`,
    { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(payload) },
  );
  return handleResponse<WizardStepResponse>(response);
}

export async function submitFinancialStep(
  workflowId: string,
  payload: OnboardingFinancialRequest,
): Promise<FinancialVerificationResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/onboarding/${encodeURIComponent(workflowId)}/financial`,
    { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(payload) },
  );
  return handleResponse<FinancialVerificationResponse>(response);
}

/**
 * Wizard step 4 (Security): stages the application and evaluates risk. Never blocks on a
 * reviewer - poll {@link getOnboardingStatus} when the response comes back with
 * `awaitingReview: true`.
 */
export async function submitSecurityStep(
  workflowId: string,
  payload: OnboardingSecurityRequest,
): Promise<AccountCreationStepResponse> {
  const response = await fetch(
    `${ORCHESTRATION_API_BASE}/api/orchestration/onboarding/${encodeURIComponent(workflowId)}/security`,
    { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(payload) },
  );
  return handleResponse<AccountCreationStepResponse>(response);
}
