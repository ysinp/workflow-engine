import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { ApiError, getOnboardingStatus, listPendingReviews, submitReviewDecision } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Field from '../components/Field';
import StatusBadge from '../components/StatusBadge';
import type { PendingReviewItem, SubmissionStatusResponse } from '../types';

type LookupPhase = 'idle' | 'loading' | 'found' | 'not-found' | 'error';
type DecisionPhase = 'idle' | 'submitting' | 'done' | 'error';
type QueuePhase = 'loading' | 'loaded' | 'error';

const QUEUE_POLL_MS = 8000;

export default function ReviewConsole() {
  const location = useLocation();
  const { token, account } = useAuth();
  const prefill = (location.state as { workflowId?: string } | null)?.workflowId ?? '';

  const [queue, setQueue] = useState<PendingReviewItem[]>([]);
  const [queuePhase, setQueuePhase] = useState<QueuePhase>('loading');

  const [workflowId, setWorkflowId] = useState(prefill);
  const [lookupPhase, setLookupPhase] = useState<LookupPhase>('idle');
  const [status, setStatus] = useState<SubmissionStatusResponse | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);

  const [reviewerId, setReviewerId] = useState(account?.email ?? '');
  const [notes, setNotes] = useState('');
  const [decisionPhase, setDecisionPhase] = useState<DecisionPhase>('idle');
  const [decisionMade, setDecisionMade] = useState<'APPROVE' | 'REJECT' | null>(null);
  const [decisionError, setDecisionError] = useState<string | null>(null);

  const refreshQueue = useCallback(async () => {
    if (!token) return;
    try {
      const items = await listPendingReviews(token);
      setQueue(items);
      setQueuePhase('loaded');
    } catch {
      setQueuePhase('error');
    }
  }, [token]);

  // Poll the pending-cases queue so newly escalated applications show up on their own -
  // a reviewer shouldn't have to already know a submission ID to find them.
  useEffect(() => {
    void refreshQueue();
    const interval = window.setInterval(() => void refreshQueue(), QUEUE_POLL_MS);
    return () => window.clearInterval(interval);
  }, [refreshQueue]);

  const lookup = async (id: string) => {
    if (!id.trim()) return;
    setLookupPhase('loading');
    setLookupError(null);
    setDecisionPhase('idle');
    setDecisionMade(null);
    setDecisionError(null);
    try {
      const result = await getOnboardingStatus(id.trim());
      setStatus(result);
      setLookupPhase('found');
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setStatus(null);
        setLookupPhase('not-found');
        return;
      }
      setLookupError(err instanceof Error ? err.message : 'Failed to look up this submission.');
      setLookupPhase('error');
    }
  };

  // Auto-lookup once if we arrived here via the "Open Review Console" link with an id in hand.
  useEffect(() => {
    if (prefill) void lookup(prefill);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleLookupSubmit = (e: FormEvent) => {
    e.preventDefault();
    void lookup(workflowId);
  };

  const handleSelectFromQueue = (id: string) => {
    setWorkflowId(id);
    void lookup(id);
  };

  const handleDecision = async (decision: 'APPROVE' | 'REJECT') => {
    if (!workflowId.trim() || !reviewerId.trim() || !token) return;
    setDecisionPhase('submitting');
    setDecisionError(null);
    try {
      await submitReviewDecision(
        workflowId.trim(),
        { decision, reviewerId: reviewerId.trim(), notes: notes.trim() || undefined },
        token,
      );
      setDecisionMade(decision);
      setDecisionPhase('done');
      // Refresh so the panel below reflects the now-resolved status, and drop it from the queue.
      void lookup(workflowId);
      void refreshQueue();
    } catch (err) {
      setDecisionError(err instanceof Error ? err.message : 'Failed to submit the decision.');
      setDecisionPhase('error');
    }
  };

  const awaitingReview = status?.status === 'AWAITING_REVIEW';

  return (
    <div className="page page-narrow">
      <section className="card">
        <p className="eyebrow">Internal tool</p>
        <h1>Compliance Review Console</h1>
        <p className="lead">
          Cases automated screening has escalated appear below. Pick one, or look a submission up
          directly by its workflow ID, and record an approve/reject decision.
        </p>

        <div className="review-queue">
          <div className="review-queue-header">
            <h2 className="review-title">Pending review ({queue.length})</h2>
          </div>
          {queuePhase === 'loading' && queue.length === 0 && (
            <div className="table-state">
              <div className="spinner" aria-hidden="true" />
              <p>Loading pending cases…</p>
            </div>
          )}
          {queuePhase === 'error' && (
            <div className="notice notice-danger">
              <p className="notice-text">Could not load the pending-review queue.</p>
            </div>
          )}
          {queuePhase !== 'loading' && queue.length === 0 && queuePhase !== 'error' && (
            <div className="table-state">
              <p className="empty-text">No applications are currently awaiting review.</p>
            </div>
          )}
          {queue.length > 0 && (
            <ul className="review-queue-list">
              {queue.map((item) => (
                <li key={item.workflowId}>
                  <button
                    type="button"
                    className={`review-queue-item ${workflowId === item.workflowId ? 'active' : ''}`}
                    onClick={() => handleSelectFromQueue(item.workflowId)}
                  >
                    <span className="mono review-queue-id">{item.workflowId}</span>
                    <span className="risk-panel-meta">
                      <StatusBadge status={item.pendingRiskAssessment.riskLevel} />
                      <span>Score {item.pendingRiskAssessment.score}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <form className="wizard-form" onSubmit={handleLookupSubmit} noValidate>
          <div className="form-grid">
            <Field id="workflowId" label="Workflow ID" span={2}>
              <input
                id="workflowId"
                type="text"
                value={workflowId}
                onChange={(e) => setWorkflowId(e.target.value)}
                placeholder="onboarding-…"
                className="mono"
                autoComplete="off"
                spellCheck={false}
              />
            </Field>
          </div>
          <div className="wizard-actions">
            <span className="wizard-actions-note" />
            <button type="submit" className="btn btn-primary" disabled={lookupPhase === 'loading'}>
              {lookupPhase === 'loading' && <span className="spinner spinner-inverse" aria-hidden="true" />}
              Look up
            </button>
          </div>
        </form>

        {lookupPhase === 'not-found' && (
          <div className="notice notice-neutral">
            <p className="notice-title">No submission found</p>
            <p className="notice-text">Check the ID and try again.</p>
          </div>
        )}

        {lookupPhase === 'error' && (
          <div className="notice notice-danger" role="alert">
            <p className="notice-title">Lookup failed</p>
            <p className="notice-text">{lookupError}</p>
          </div>
        )}

        {status && lookupPhase === 'found' && (
          <div className="review-box">
            <div className="review-box-header">
              <h2 className="review-title">Status</h2>
              <StatusBadge status={status.status} />
            </div>

            {status.status === 'AWAITING_REVIEW' && status.pendingRiskAssessment && (
              <div className="risk-panel risk-panel-review">
                <div className="risk-panel-header">
                  <div>
                    <p className="risk-panel-title">Flagged for manual review</p>
                    <p className="risk-panel-text">
                      Automated screening produced the score and reasons below. Decide whether to
                      approve or reject this application.
                    </p>
                  </div>
                  <div className="risk-score" aria-label={`Risk score ${status.pendingRiskAssessment.score}`}>
                    <span className="risk-score-value">{status.pendingRiskAssessment.score}</span>
                    <span className="risk-score-label">Risk score</span>
                  </div>
                </div>
                {status.pendingRiskAssessment.reasons.length > 0 && (
                  <ul className="risk-reasons">
                    {status.pendingRiskAssessment.reasons.map((reason, i) => (
                      <li key={`${i}-${reason}`}>{reason}</li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {status.status === 'COMPLETED' && status.result && (
              <dl className="summary-grid">
                <div>
                  <dt>Outcome</dt>
                  <dd>
                    <StatusBadge status={status.result.outcome} />
                  </dd>
                </div>
                <div>
                  <dt>Risk level</dt>
                  <dd>{status.result.riskLevel}</dd>
                </div>
              </dl>
            )}

            {status.status === 'RUNNING' && (
              <div className="notice notice-info">
                <div className="spinner" aria-hidden="true" />
                <p className="notice-text">Still being processed - not yet ready for review.</p>
              </div>
            )}

            {status.status === 'FAILED' && (
              <div className="notice notice-danger">
                <p className="notice-text">{status.errorMessage ?? 'The workflow failed.'}</p>
              </div>
            )}

            {awaitingReview && decisionPhase !== 'done' && (
              <div className="form-grid" style={{ marginTop: '1.5rem' }}>
                <Field id="reviewerId" label="Reviewer name / ID">
                  <input
                    id="reviewerId"
                    type="text"
                    value={reviewerId}
                    onChange={(e) => setReviewerId(e.target.value)}
                    placeholder="e.g. jane.reviewer"
                    autoComplete="off"
                  />
                </Field>
                <Field id="notes" label="Notes" optional span={2}>
                  <textarea
                    id="notes"
                    rows={3}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Rationale for this decision…"
                  />
                </Field>
                <div className="field-span-2 button-row">
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={!reviewerId.trim() || decisionPhase === 'submitting'}
                    onClick={() => void handleDecision('APPROVE')}
                  >
                    {decisionPhase === 'submitting' && decisionMade !== 'REJECT' && (
                      <span className="spinner spinner-inverse" aria-hidden="true" />
                    )}
                    Approve
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    disabled={!reviewerId.trim() || decisionPhase === 'submitting'}
                    onClick={() => void handleDecision('REJECT')}
                  >
                    Reject
                  </button>
                </div>
                {decisionPhase === 'error' && (
                  <div className="notice notice-danger field-span-2" role="alert">
                    <p className="notice-text">{decisionError}</p>
                  </div>
                )}
              </div>
            )}

            {decisionPhase === 'done' && decisionMade && (
              <div className={`notice ${decisionMade === 'APPROVE' ? 'notice-info' : 'notice-warning'}`}>
                <p className="notice-title">
                  Decision recorded: {decisionMade === 'APPROVE' ? 'Approved' : 'Rejected'}
                </p>
                <p className="notice-text">The workflow has resumed and will complete shortly.</p>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
