import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  ApiError,
  getOnboardingStatus,
  submitFinancialStep,
  submitIdentificationStep,
  submitPersonalContactStep,
  submitSecurityStep,
} from '../api/client';
import Field from '../components/Field';
import StatusBadge from '../components/StatusBadge';
import StepProgress from '../components/StepProgress';
import {
  ANNUAL_INCOMES,
  COUNTRIES,
  GENDERS,
  ID_TYPES,
  Option,
  SOURCES_OF_FUNDS,
  annualIncomeLabel,
  countryLabel,
} from '../constants/kyc';
import type {
  AccountCreationStepResponse,
  AnnualIncome,
  IdType,
  KycProfile,
  OnboardingFinancialRequest,
  OnboardingIdentificationRequest,
  OnboardingPersonalContactRequest,
  RiskAssessmentResult,
  SourceOfFunds,
} from '../types';

const POLL_INTERVAL_MS = 1000;

/**
 * URL slugs the wizard navigates through - each step is its own route
 * (/signup/create-new/:step) so backend-driven progress (the `nextActivity` in the orchestration
 * engine's responses, naming the gate activity now pending) is reflected directly in the address
 * bar, not just in memory.
 */
const STEP_SLUGS = ['personal', 'identification', 'financial', 'security'] as const;
type StepSlug = (typeof STEP_SLUGS)[number];

const STEPS = ['Personal & Contact', 'Identification', 'Financial', 'Security'];

const STEP_INTRO: { title: string; subtitle: string; why: string }[] = [
  {
    title: 'Tell us about yourself',
    subtitle: 'Your personal details and contact information.',
    why: 'Regulations require us to verify the identity, address, and contact details of every customer before opening an account.',
  },
  {
    title: 'Verify your identity',
    subtitle: 'Provide a government-issued identity document.',
    why: 'Your document is screened against sanctions and watch lists as part of standard KYC checks.',
  },
  {
    title: 'Your financial profile',
    subtitle: 'Help us understand how you plan to use your account - we verify this against your identification.',
    why: 'Understanding your identity alongside your source of funds is part of our anti-money-laundering obligations.',
  },
  {
    title: 'Secure your account',
    subtitle: 'Choose a password, review your details and accept the terms.',
    why: 'Your password is encrypted before it is stored. We never share it with anyone.',
  },
];

interface FormState {
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
  idType: IdType | '';
  idNumber: string;
  occupation: string;
  annualIncome: AnnualIncome | '';
  sourceOfFunds: SourceOfFunds | '';
  password: string;
  confirmPassword: string;
  acceptTerms: boolean;
}

type FieldName = keyof FormState;
type Errors = Partial<Record<FieldName, string>>;

const EMPTY_FORM: FormState = {
  fullName: '',
  dateOfBirth: '',
  nationality: '',
  gender: '',
  email: '',
  phone: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: '',
  idType: '',
  idNumber: '',
  occupation: '',
  annualIncome: '',
  sourceOfFunds: '',
  password: '',
  confirmPassword: '',
  acceptTerms: false,
};

/** Which step each field lives on - used to jump to the first step with an error. */
const FIELD_STEP: Record<FieldName, number> = {
  fullName: 0,
  dateOfBirth: 0,
  nationality: 0,
  gender: 0,
  email: 0,
  phone: 0,
  addressLine1: 0,
  addressLine2: 0,
  city: 0,
  state: 0,
  postalCode: 0,
  country: 0,
  idType: 1,
  idNumber: 1,
  occupation: 2,
  annualIncome: 2,
  sourceOfFunds: 2,
  password: 3,
  confirmPassword: 3,
  acceptTerms: 3,
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function todayIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

function stepFromSlug(slug: string | undefined): number {
  const index = STEP_SLUGS.indexOf((slug ?? 'personal') as StepSlug);
  return index === -1 ? 0 : index;
}

function validateStep(step: number, f: FormState): Errors {
  const e: Errors = {};
  switch (step) {
    case 0:
      // Personal
      if (!f.fullName.trim()) e.fullName = 'Enter your full legal name.';
      if (!f.dateOfBirth) e.dateOfBirth = 'Enter your date of birth.';
      else if (f.dateOfBirth > todayIso()) e.dateOfBirth = 'Date of birth cannot be in the future.';
      if (!f.nationality) e.nationality = 'Select your nationality.';
      // Contact
      if (!f.email.trim()) e.email = 'Enter your email address.';
      else if (!EMAIL_RE.test(f.email.trim())) e.email = 'Enter a valid email address.';
      if (!f.addressLine1.trim()) e.addressLine1 = 'Enter your street address.';
      if (!f.city.trim()) e.city = 'Enter your city.';
      if (!f.country) e.country = 'Select your country of residence.';
      break;
    case 1:
      if (!f.idType) e.idType = 'Select an ID document type.';
      if (!f.idNumber.trim()) e.idNumber = 'Enter your ID document number.';
      break;
    case 2:
      // Lenient: occupation is optional, but the two enum selects are needed
      // so the payload always carries a valid contract value.
      if (!f.annualIncome) e.annualIncome = 'Select an income range.';
      if (!f.sourceOfFunds) e.sourceOfFunds = 'Select your primary source of funds.';
      break;
    case 3:
      if (f.password.length < 8) e.password = 'Password must be at least 8 characters.';
      if (!f.confirmPassword) e.confirmPassword = 'Re-enter your password.';
      else if (f.confirmPassword !== f.password) e.confirmPassword = 'Passwords do not match.';
      if (!f.acceptTerms) e.acceptTerms = 'You must accept the terms to continue.';
      break;
  }
  return e;
}

function toKycProfile(f: FormState): KycProfile {
  return {
    fullName: f.fullName.trim(),
    email: f.email.trim(),
    dateOfBirth: f.dateOfBirth,
    nationality: f.nationality,
    phone: f.phone.trim(),
    addressLine1: f.addressLine1.trim(),
    addressLine2: f.addressLine2.trim(),
    city: f.city.trim(),
    state: f.state.trim(),
    postalCode: f.postalCode.trim(),
    country: f.country,
    idType: f.idType as IdType,
    idNumber: f.idNumber.trim(),
    occupation: f.occupation.trim(),
    annualIncome: f.annualIncome as AnnualIncome,
    sourceOfFunds: f.sourceOfFunds as SourceOfFunds,
  };
}

function toPersonalContactRequest(f: FormState): OnboardingPersonalContactRequest {
  return {
    fullName: f.fullName.trim(),
    dateOfBirth: f.dateOfBirth,
    nationality: f.nationality,
    gender: f.gender,
    email: f.email.trim(),
    phone: f.phone.trim(),
    addressLine1: f.addressLine1.trim(),
    addressLine2: f.addressLine2.trim(),
    city: f.city.trim(),
    state: f.state.trim(),
    postalCode: f.postalCode.trim(),
    country: f.country,
  };
}

function toIdentificationRequest(f: FormState): OnboardingIdentificationRequest {
  return {
    idType: f.idType as IdType,
    idNumber: f.idNumber.trim(),
  };
}

function toFinancialRequest(f: FormState): OnboardingFinancialRequest {
  return {
    occupation: f.occupation.trim(),
    annualIncome: f.annualIncome as AnnualIncome,
    sourceOfFunds: f.sourceOfFunds as SourceOfFunds,
  };
}

function maskTail(value: string): string {
  const v = value.trim();
  if (v.length <= 4) return v;
  return `•••• ${v.slice(-4)}`;
}

function optionLabel(options: Option[], value: string): string {
  return options.find((o) => o.value === value)?.label ?? '—';
}

type RiskPhase = 'idle' | 'checking' | 'awaiting_review' | 'completed' | 'failed';

interface PendingAssessment {
  riskLevel: 'LOW' | 'HIGH';
  score: number;
  reasons: string[];
}

export default function SignUp() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams<{ step?: string }>();
  const step = stepFromSlug(params.step);

  // Carried over from ProductSelect's navigation state - captured once on mount (a lazy
  // initializer) so it survives this component's own internal step-to-step navigations, which
  // don't re-pass state. Presentational only: every product leads into this same wizard and
  // account-creation flow; this is never sent to any backend, only shown back to the user.
  const [selectedProduct] = useState<string | null>(
    () => (location.state as { product?: string } | null)?.product ?? null,
  );

  // The running OnboardingWizardWorkflow execution this wizard drives via Update calls - started
  // by ProductSelect's "Get started" click. Captured once on mount for the same reason as
  // selectedProduct above; redirected back to product selection if missing (e.g. a page refresh
  // mid-wizard, which has no way to resume the workflow without re-navigating from its start).
  const [workflowId] = useState<string | null>(
    () => (location.state as { workflowId?: string } | null)?.workflowId ?? null,
  );

  useEffect(() => {
    if (!workflowId) navigate('/signup/create-new', { replace: true });
  }, [workflowId, navigate]);

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // kyc-service's rejection reasons (step 1) - blocks advancing until cleared by a passing re-check.
  const [verificationReasons, setVerificationReasons] = useState<string[] | null>(null);

  // Snapshot of what was submitted - there is no server-issued Account to display until the
  // risk-assessment workflow actually approves this application.
  const [submittedProfile, setSubmittedProfile] = useState<KycProfile | null>(null);
  const [submittedAt, setSubmittedAt] = useState<string | null>(null);

  // Risk assessment, now resolved directly against this same onboarding workflow: the Security
  // step's submitSecurityStep call returns the risk verdict synchronously (LOW auto-approves
  // immediately; HIGH comes back with awaitingReview=true), so polling only ever happens while
  // genuinely waiting on a human reviewer's decision.
  const [riskPhase, setRiskPhase] = useState<RiskPhase>('idle');
  const [riskResult, setRiskResult] = useState<RiskAssessmentResult | null>(null);
  const [riskError, setRiskError] = useState<string | null>(null);
  const [pendingAssessment, setPendingAssessment] = useState<PendingAssessment | null>(null);

  const intervalRef = useRef<number | null>(null);
  const pollGenerationRef = useRef(0);
  const headingRef = useRef<HTMLHeadingElement | null>(null);

  const clearTimers = () => {
    if (intervalRef.current !== null) {
      window.clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  useEffect(
    () => () => {
      pollGenerationRef.current += 1;
      clearTimers();
    },
    [],
  );

  // Move focus to the step heading on step change (keyboard/screen-reader friendly).
  // Only when the step actually changes, so the page doesn't steal focus on load.
  const prevStepRef = useRef(step);
  useEffect(() => {
    if (prevStepRef.current === step) return;
    prevStepRef.current = step;
    headingRef.current?.focus();
  }, [step]);

  const update =
    (name: FieldName) =>
    (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      const target = e.target as HTMLInputElement;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      setForm((prev) => ({ ...prev, [name]: value }));
      if (errors[name]) {
        setErrors((prev) => {
          const next = { ...prev };
          delete next[name];
          return next;
        });
      }
    };

  /** Common props for text-like inputs and selects. */
  const bind = (name: Exclude<FieldName, 'acceptTerms'>) => ({
    id: name,
    name,
    value: form[name],
    onChange: update(name),
    disabled: submitting,
    'aria-invalid': errors[name] ? true : undefined,
    'aria-describedby': `${name}-msg`,
  });

  // -------------------------------------------------------------------------
  // Reviewer-decision polling - only ever runs while awaiting_review; a real reviewer may take
  // any amount of time, so this polls indefinitely (no timeout) until COMPLETED or FAILED.
  // -------------------------------------------------------------------------

  const startReviewPolling = () => {
    if (!workflowId) return;
    clearTimers();
    const generation = ++pollGenerationRef.current;

    const poll = async () => {
      try {
        const status = await getOnboardingStatus(workflowId);
        if (generation !== pollGenerationRef.current) return; // stale
        if (status.status === 'COMPLETED') {
          clearTimers();
          setRiskResult(status.result ?? null);
          setRiskPhase('completed');
        } else if (status.status === 'FAILED') {
          clearTimers();
          setRiskError(status.errorMessage ?? 'The compliance review could not be completed.');
          setRiskPhase('failed');
        }
        // RUNNING/AWAITING_REVIEW: nothing changed yet, keep polling.
      } catch (err) {
        if (generation !== pollGenerationRef.current) return;
        clearTimers();
        setRiskError(err instanceof Error ? err.message : 'Failed to check the review status.');
        setRiskPhase('failed');
      }
    };

    intervalRef.current = window.setInterval(poll, POLL_INTERVAL_MS);
    void poll();
  };

  /** Applies the Security step's response: LOW resolves immediately, HIGH starts review polling. */
  const applyStepResult = (result: AccountCreationStepResponse) => {
    if (result.awaitingReview) {
      setPendingAssessment({ riskLevel: result.riskLevel, score: result.score, reasons: result.reasons });
      setRiskPhase('awaiting_review');
      startReviewPolling();
    } else {
      setRiskResult(result.result ?? null);
      setRiskPhase('completed');
    }
  };

  /** Re-submits the Security step after a FAILED phase (e.g. a transient service outage). */
  const retrySubmission = async () => {
    if (!workflowId) return;
    setRiskPhase('checking');
    setRiskError(null);
    setRiskResult(null);
    try {
      const result = await submitSecurityStep(workflowId, { password: form.password });
      applyStepResult(result);
    } catch (err) {
      setRiskError(err instanceof Error ? err.message : 'The application could not be submitted.');
      setRiskPhase('failed');
    }
  };

  // -------------------------------------------------------------------------
  // Navigation + submit
  // -------------------------------------------------------------------------

  // Every step here is backed by its own genuine, parked Temporal activity (see
  // OnboardingWizardWorkflow): once completed, it cannot be reopened - the workflow only ever
  // schedules the next one in sequence, and the backend rejects a resubmission outright (409).
  // There is deliberately no "go back" navigation to match: the wizard only ever moves forward.
  const goToStep = (index: number) => {
    navigate(`/signup/create-new/${STEP_SLUGS[index]}`);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!workflowId) {
      setSubmitError('Something went wrong starting your application - please start over.');
      return;
    }

    // Step 1: Personal + Contact -> orchestration engine Update (delegates to identification-service)
    if (step === 0) {
      const stepErrors = validateStep(0, form);
      setErrors(stepErrors);
      if (Object.keys(stepErrors).length > 0) return;

      setSubmitting(true);
      setSubmitError(null);
      try {
        const saved = await submitPersonalContactStep(workflowId, toPersonalContactRequest(form));
        setSubmitting(false);
        goToStep(STEP_SLUGS.indexOf(saved.nextActivity as StepSlug));
      } catch (err) {
        setSubmitting(false);
        setSubmitError(
          err instanceof ApiError ? err.message : 'We could not save your details. Please try again.',
        );
      }
      return;
    }

    // Step 2: Identification -> orchestration engine Update (delegates to identification-service's PATCH)
    if (step === 1) {
      const stepErrors = validateStep(1, form);
      setErrors(stepErrors);
      if (Object.keys(stepErrors).length > 0) return;

      setSubmitting(true);
      setSubmitError(null);
      try {
        const updated = await submitIdentificationStep(workflowId, toIdentificationRequest(form));
        setSubmitting(false);
        goToStep(STEP_SLUGS.indexOf(updated.nextActivity as StepSlug));
      } catch (err) {
        setSubmitting(false);
        setSubmitError(
          err instanceof ApiError ? err.message : 'We could not save your identification. Please try again.',
        );
      }
      return;
    }

    // Step 3: Financial + KYC verification -> orchestration engine Update (delegates to kyc-service)
    if (step === 2) {
      const stepErrors = validateStep(2, form);
      setErrors(stepErrors);
      if (Object.keys(stepErrors).length > 0) return;

      setSubmitting(true);
      setSubmitError(null);
      setVerificationReasons(null);
      try {
        const result = await submitFinancialStep(workflowId, toFinancialRequest(form));
        setSubmitting(false);
        if (!result.verified || !result.nextActivity) {
          setVerificationReasons(result.reasons);
          return;
        }
        goToStep(STEP_SLUGS.indexOf(result.nextActivity as StepSlug));
      } catch (err) {
        setSubmitting(false);
        setSubmitError(
          err instanceof ApiError ? err.message : 'We could not verify your details. Please try again.',
        );
      }
      return;
    }

    // Step 4: Security -> orchestration engine Update (stages the application and evaluates risk
    // on this same workflow; a HIGH verdict pauses it for a compliance reviewer).
    const allErrors: Errors = {};
    for (let i = 0; i < STEP_SLUGS.length; i += 1) Object.assign(allErrors, validateStep(i, form));
    if (Object.keys(allErrors).length > 0) {
      setErrors(allErrors);
      const firstStep = Math.min(...Object.keys(allErrors).map((k) => FIELD_STEP[k as FieldName]));
      goToStep(firstStep);
      return;
    }

    setErrors({});
    setSubmitError(null);
    setSubmitting(true);

    const profile = toKycProfile(form);
    try {
      const result = await submitSecurityStep(workflowId, { password: form.password });
      setSubmittedProfile(profile);
      setSubmittedAt(new Date().toISOString());
      setSubmitting(false);
      window.scrollTo({ top: 0, behavior: 'smooth' });
      navigate('/signup/create-new/submitted');
      applyStepResult(result);
    } catch (err) {
      setSubmitting(false);
      setSubmitError(
        err instanceof ApiError
          ? err.message
          : 'We could not submit your application. Please try again.',
      );
    }
  };

  const handleStartOver = () => {
    pollGenerationRef.current += 1;
    clearTimers();
    setForm(EMPTY_FORM);
    setVerificationReasons(null);
    setErrors({});
    setSubmitError(null);
    setSubmittedProfile(null);
    setSubmittedAt(null);
    setRiskPhase('idle');
    setRiskResult(null);
    setRiskError(null);
    setPendingAssessment(null);
    // A new application needs a fresh product selection and a fresh workflow, not just the
    // first wizard step - the old workflowId (and whatever it already completed) is done with.
    navigate('/signup/create-new');
  };

  // -------------------------------------------------------------------------
  // Render: application submitted (awaiting a risk decision that gates account creation)
  // -------------------------------------------------------------------------

  if (submittedProfile) {
    const approved = riskPhase === 'completed' && riskResult?.outcome === 'APPROVED';
    const rejected = riskPhase === 'completed' && riskResult?.outcome === 'REJECTED';
    const firstName = submittedProfile.fullName.split(' ')[0] || submittedProfile.fullName;

    const heroTitle = approved
      ? 'Account approved!'
      : rejected
        ? 'Application rejected'
        : 'Reviewing your application';
    const heroLead = approved ? (
      <>Welcome, {firstName}. Your account has been created and is ready to use.</>
    ) : rejected ? (
      'Unfortunately this application did not pass compliance review. No account was created.'
    ) : (
      <>Thanks, {firstName}. We're verifying your details before your account is created.</>
    );

    return (
      <div className="page page-narrow">
        <section className="card result-card" aria-live="polite">
          <div className="result-hero">
            <div className="result-hero-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="28" height="28">
                <path
                  d="M5 12.5l4.5 4.5L19 7.5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <div>
              <p className="eyebrow">{selectedProduct ?? 'Application submitted'}</p>
              <h1 className="result-title">{heroTitle}</h1>
              <p className="lead">{heroLead}</p>
            </div>
          </div>

          <ol className="timeline">
            <li className="timeline-item timeline-item-done">
              <span className="timeline-marker" aria-hidden="true" />
              <div className="timeline-body">
                <div className="timeline-heading">
                  <h2 className="timeline-title">Application submitted</h2>
                  <StatusBadge status="COMPLETED" label="Received" />
                </div>
                <p className="timeline-text">
                  Received by the accounts service and staged for a compliance decision - no account exists yet.
                </p>
                <dl className="summary-grid">
                  <div>
                    <dt>Full name</dt>
                    <dd>{submittedProfile.fullName}</dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{submittedProfile.email}</dd>
                  </div>
                  <div>
                    <dt>ID document</dt>
                    <dd className="mono">{maskTail(submittedProfile.idNumber) || '—'}</dd>
                  </div>
                  <div>
                    <dt>Submitted</dt>
                    <dd>{submittedAt ? new Date(submittedAt).toLocaleString() : '—'}</dd>
                  </div>
                </dl>
              </div>
            </li>

            <li
              className={`timeline-item ${
                riskPhase === 'completed'
                  ? 'timeline-item-done'
                  : riskPhase === 'failed'
                    ? 'timeline-item-warn'
                    : 'timeline-item-active'
              }`}
            >
              <span className="timeline-marker" aria-hidden="true" />
              <div className="timeline-body">
                <div className="timeline-heading">
                  <h2 className="timeline-title">Risk assessment &amp; decision</h2>
                  {riskPhase === 'completed' && riskResult && (
                    <StatusBadge
                      status={riskResult.outcome}
                      label={riskResult.outcome === 'APPROVED' ? 'Approved' : 'Rejected'}
                    />
                  )}
                  {riskPhase === 'checking' && <StatusBadge status="RUNNING" label="Running" />}
                  {riskPhase === 'awaiting_review' && (
                    <StatusBadge status="AWAITING_REVIEW" label="Under review" />
                  )}
                  {riskPhase === 'failed' && <StatusBadge status="FAILED" label="Unavailable" />}
                </div>
                <p className="timeline-text">
                  A compliance workflow evaluates this application and its outcome determines whether your
                  account is created.
                </p>

                {riskPhase === 'checking' && (
                  <div className="notice notice-info">
                    <div className="spinner" aria-hidden="true" />
                    <div>
                      <p className="notice-title">Reviewing your application…</p>
                      <p className="notice-text">
                        Workflow <code>{workflowId}</code> is evaluating your application.
                      </p>
                    </div>
                  </div>
                )}

                {riskPhase === 'awaiting_review' && (
                  <div className="notice notice-warning">
                    <div>
                      <p className="notice-title">Waiting update from Compliance Officer</p>
                      <p className="notice-text">
                        Automated screening flagged this application for manual review. This page will
                        update automatically the moment a reviewer records a decision.
                      </p>
                      {pendingAssessment && (
                        <ul className="risk-reasons">
                          {pendingAssessment.reasons.map((reason, i) => (
                            <li key={`${i}-${reason}`}>{reason}</li>
                          ))}
                        </ul>
                      )}
                      {workflowId && (
                        <Link
                          to="/review"
                          state={{ workflowId }}
                          className="btn btn-secondary btn-sm"
                        >
                          Open Review Console
                        </Link>
                      )}
                    </div>
                  </div>
                )}

                {riskPhase === 'completed' && riskResult && (
                  <RiskResultPanel result={riskResult} />
                )}

                {riskPhase === 'completed' && !riskResult && (
                  <div className="notice notice-neutral">
                    <p className="notice-title">Assessment completed</p>
                    <p className="notice-text">The workflow finished without returning a result.</p>
                  </div>
                )}

                {riskPhase === 'failed' && (
                  <div className="notice notice-warning">
                    <div>
                      <p className="notice-title">We couldn't complete your application</p>
                      <p className="notice-text">{riskError}</p>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => void retrySubmission()}
                      >
                        Retry
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </li>
          </ol>

          <div className="result-actions">
            {approved && (
              <Link to="/signin" state={{ email: submittedProfile.email }} className="btn btn-primary btn-lg">
                Sign In
              </Link>
            )}
            {(approved || rejected || riskPhase === 'failed') && (
              <button
                type="button"
                className={approved ? 'btn btn-ghost' : 'btn btn-primary btn-lg'}
                onClick={handleStartOver}
              >
                Start a new application
              </button>
            )}
          </div>
        </section>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // Render: wizard
  // -------------------------------------------------------------------------

  const intro = STEP_INTRO[step];
  const isLast = step === STEP_SLUGS.length - 1;

  return (
    <div className="page onboarding">
      <aside className="onboarding-aside">
        <p className="eyebrow eyebrow-light">{selectedProduct ?? 'Open an account'}</p>
        <h2 className="aside-title">Digital onboarding in under five minutes.</h2>
        <p className="aside-text">
          Complete your identity verification online. Your account is opened as soon as you finish.
        </p>
        <div className="aside-why">
          <p className="aside-why-label">Why we ask</p>
          <p className="aside-why-text">{intro.why}</p>
        </div>
      </aside>

      <section className="card wizard-card">
        <StepProgress steps={STEPS} current={step} />

        <header className="wizard-header">
          <p className="eyebrow">
            Step {step + 1} of {STEPS.length}
          </p>
          <h1 className="wizard-title" tabIndex={-1} ref={headingRef}>
            {intro.title}
          </h1>
          <p className="wizard-subtitle">{intro.subtitle}</p>
        </header>

        <form className="wizard-form" onSubmit={handleSubmit} noValidate>
          {step === 0 && (
            <>
              <h2 className="wizard-section-title">Personal</h2>
              <div className="form-grid">
                <Field id="fullName" label="Full legal name" error={errors.fullName} span={2}>
                  <input {...bind('fullName')} type="text" autoComplete="name" placeholder="Jane Alexandra Doe" />
                </Field>
                <div className="form-grid-trio">
                  <Field id="dateOfBirth" label="Date of birth" error={errors.dateOfBirth}>
                    <input {...bind('dateOfBirth')} type="date" autoComplete="bday" max={todayIso()} />
                  </Field>
                  <Field id="nationality" label="Nationality" error={errors.nationality}>
                    <select {...bind('nationality')}>
                      <option value="">Select a country</option>
                      {COUNTRIES.map((c) => (
                        <option key={c.value} value={c.value}>
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field id="gender" label="Gender" optional>
                    <select {...bind('gender')}>
                      <option value="">Select</option>
                      {GENDERS.map((g) => (
                        <option key={g.value} value={g.value}>
                          {g.label}
                        </option>
                      ))}
                    </select>
                  </Field>
                </div>
              </div>

              <h2 className="wizard-section-title">Contact</h2>
              <div className="form-grid">
                <Field id="email" label="Email address" error={errors.email}>
                  <input {...bind('email')} type="email" autoComplete="email" placeholder="jane.doe@example.com" />
                </Field>
                <Field id="phone" label="Phone number" optional>
                  <input {...bind('phone')} type="tel" autoComplete="tel" placeholder="+1 555 010 0199" />
                </Field>
                <Field id="addressLine1" label="Address line 1" error={errors.addressLine1} span={2}>
                  <input
                    {...bind('addressLine1')}
                    type="text"
                    autoComplete="address-line1"
                    placeholder="Street address"
                  />
                </Field>
                <Field id="addressLine2" label="Address line 2" optional span={2}>
                  <input
                    {...bind('addressLine2')}
                    type="text"
                    autoComplete="address-line2"
                    placeholder="Apartment, suite, unit"
                  />
                </Field>
                <Field id="city" label="City" error={errors.city}>
                  <input {...bind('city')} type="text" autoComplete="address-level2" />
                </Field>
                <Field id="state" label="State / Province" optional>
                  <input {...bind('state')} type="text" autoComplete="address-level1" />
                </Field>
                <Field id="postalCode" label="Postal code" optional>
                  <input {...bind('postalCode')} type="text" autoComplete="postal-code" />
                </Field>
                <Field id="country" label="Country of residence" error={errors.country}>
                  <select {...bind('country')} autoComplete="country">
                    <option value="">Select a country</option>
                    {COUNTRIES.map((c) => (
                      <option key={c.value} value={c.value}>
                        {c.label}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
            </>
          )}

          {step === 1 && (
            <div className="form-grid">
              <Field id="idType" label="Document type" error={errors.idType}>
                <select {...bind('idType')}>
                  <option value="">Select a document</option>
                  {ID_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </Field>
              <Field
                id="idNumber"
                label="Document number"
                error={errors.idNumber}
                hint="Demo environment: enter any sample document number. No real ID is required."
              >
                <input
                  {...bind('idNumber')}
                  type="text"
                  autoComplete="off"
                  spellCheck={false}
                  placeholder="e.g. X12345678 (demo)"
                />
              </Field>
              <div className="field-span-2 info-callout">
                <strong>Your data is protected.</strong> Only the last four digits of your document number are
                displayed after submission.
              </div>
            </div>
          )}

          {step === 2 && (
            <>
              {verificationReasons && verificationReasons.length > 0 && (
                <div className="notice notice-warning" role="alert">
                  <div>
                    <p className="notice-title">We couldn't verify your details</p>
                    <p className="notice-text">Please check the following before continuing:</p>
                    <ul className="risk-reasons">
                      {verificationReasons.map((reason, i) => (
                        <li key={`${i}-${reason}`}>{reason}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}
              <div className="form-grid">
                <Field id="occupation" label="Occupation" optional span={2}>
                  <input
                    {...bind('occupation')}
                    type="text"
                    autoComplete="organization-title"
                    placeholder="e.g. Software Engineer"
                  />
                </Field>
                <Field id="annualIncome" label="Annual income" error={errors.annualIncome}>
                  <select {...bind('annualIncome')}>
                    <option value="">Select a range</option>
                    {ANNUAL_INCOMES.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field id="sourceOfFunds" label="Source of funds" error={errors.sourceOfFunds}>
                  <select {...bind('sourceOfFunds')}>
                    <option value="">Select a source</option>
                    {SOURCES_OF_FUNDS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
            </>
          )}

          {step === 3 && (
            <>
              <div className="review-box">
                <div className="review-box-header">
                  <h2 className="review-title">Review your details</h2>
                </div>
                <dl className="summary-grid">
                  {selectedProduct && (
                    <div>
                      <dt>Product</dt>
                      <dd>{selectedProduct}</dd>
                    </div>
                  )}
                  <div>
                    <dt>Name</dt>
                    <dd>{form.fullName || '—'}</dd>
                  </div>
                  <div>
                    <dt>Date of birth</dt>
                    <dd>{form.dateOfBirth || '—'}</dd>
                  </div>
                  <div>
                    <dt>Nationality</dt>
                    <dd>{countryLabel(form.nationality)}</dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{form.email || '—'}</dd>
                  </div>
                  <div>
                    <dt>Residence</dt>
                    <dd>{[form.city, countryLabel(form.country)].filter(Boolean).join(', ')}</dd>
                  </div>
                  <div>
                    <dt>Document</dt>
                    <dd>
                      {optionLabel(ID_TYPES, form.idType)} {form.idNumber && <span className="mono">{maskTail(form.idNumber)}</span>}
                    </dd>
                  </div>
                  <div>
                    <dt>Annual income</dt>
                    <dd>{annualIncomeLabel(form.annualIncome)}</dd>
                  </div>
                  <div>
                    <dt>Source of funds</dt>
                    <dd>{optionLabel(SOURCES_OF_FUNDS, form.sourceOfFunds)}</dd>
                  </div>
                </dl>
              </div>

              <div className="form-grid">
                <Field
                  id="password"
                  label="Password"
                  error={errors.password}
                  hint="At least 8 characters."
                >
                  <input {...bind('password')} type="password" autoComplete="new-password" />
                </Field>
                <Field
                  id="confirmPassword"
                  label="Confirm password"
                  error={
                    errors.confirmPassword ??
                    (form.confirmPassword && form.confirmPassword !== form.password
                      ? 'Passwords do not match.'
                      : undefined)
                  }
                  hint={
                    form.confirmPassword && form.confirmPassword === form.password ? (
                      <span className="field-hint-success">Passwords match</span>
                    ) : undefined
                  }
                >
                  <input {...bind('confirmPassword')} type="password" autoComplete="new-password" />
                </Field>
                <div className={`field field-span-2${errors.acceptTerms ? ' field-invalid' : ''}`}>
                  <label className="checkbox">
                    <input
                      type="checkbox"
                      id="acceptTerms"
                      name="acceptTerms"
                      checked={form.acceptTerms}
                      onChange={update('acceptTerms')}
                      disabled={submitting}
                      aria-invalid={errors.acceptTerms ? true : undefined}
                      aria-describedby="acceptTerms-msg"
                    />
                    <span>
                      I confirm the information provided is accurate and I agree to the{' '}
                      <a href="#terms" onClick={(e) => e.preventDefault()}>
                        Terms &amp; Conditions
                      </a>{' '}
                      and{' '}
                      <a href="#privacy" onClick={(e) => e.preventDefault()}>
                        Privacy Notice
                      </a>
                      .
                    </span>
                  </label>
                  {errors.acceptTerms && (
                    <p id="acceptTerms-msg" className="field-error" role="alert">
                      {errors.acceptTerms}
                    </p>
                  )}
                </div>
              </div>

              {submitError && (
                <div className="notice notice-danger" role="alert">
                  <div>
                    <p className="notice-title">We couldn't create your account</p>
                    <p className="notice-text">{submitError}</p>
                  </div>
                </div>
              )}
            </>
          )}

          {step !== 3 && submitError && (
            <div className="notice notice-danger" role="alert">
              <div>
                <p className="notice-title">Something went wrong</p>
                <p className="notice-text">{submitError}</p>
              </div>
            </div>
          )}

          <div className="wizard-actions">
            {step === 0 ? (
              <span className="wizard-actions-note">
                Already have an account? <Link to="/signin">Sign in</Link>
              </span>
            ) : (
              <span className="wizard-actions-note" />
            )}
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting && <span className="spinner spinner-inverse" aria-hidden="true" />}
              {isLast ? (submitting ? 'Creating account…' : 'Create Account') : 'Continue'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function RiskResultPanel({ result }: { result: RiskAssessmentResult }) {
  const approved = result.outcome === 'APPROVED';
  const reviewed = result.riskLevel === 'HIGH'; // HIGH always goes through a human reviewer now
  const title = approved
    ? reviewed
      ? 'Approved — cleared by compliance review'
      : 'Approved — low risk'
    : 'Rejected — declined by compliance review';
  const text = approved
    ? reviewed
      ? 'Automated screening flagged this application, but a compliance reviewer approved it after manual review. Your account has been created.'
      : 'Automated screening found no concerns with this application. Your account has been created.'
    : 'This application was escalated for manual review and a compliance reviewer declined it. No account was created for it.';
  return (
    <div className={`risk-panel ${approved ? 'risk-panel-approved' : 'risk-panel-review'}`}>
      <div className="risk-panel-header">
        <div>
          <p className="risk-panel-title">{title}</p>
          <p className="risk-panel-text">{text}</p>
        </div>
        <div className="risk-score" aria-label={`Risk score ${result.score}`}>
          <span className="risk-score-value">{result.score}</span>
          <span className="risk-score-label">Risk score</span>
        </div>
      </div>
      <div className="risk-panel-meta">
        <span>Risk level</span>
        <StatusBadge status={result.riskLevel} label={result.riskLevel === 'LOW' ? 'Low' : 'High'} />
      </div>
      {result.reasons && result.reasons.length > 0 && (
        <ul className="risk-reasons">
          {result.reasons.map((reason, i) => (
            <li key={`${i}-${reason}`}>{reason}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
