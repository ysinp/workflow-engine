import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UnauthorizedError, fetchAccounts } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { annualIncomeLabel, countryLabel } from '../constants/kyc';
import type { Account } from '../types';

type LoadState = 'loading' | 'loaded' | 'error';

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0][0] ?? '';
  const last = parts.length > 1 ? parts[parts.length - 1][0] ?? '' : '';
  return (first + last).toUpperCase();
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export default function Dashboard() {
  const { token, account: me, logout } = useAuth();
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    setLoadState('loading');
    setError(null);
    try {
      const data = await fetchAccounts(token);
      setAccounts(data);
      setLoadState('loaded');
    } catch (err) {
      if (err instanceof UnauthorizedError) {
        // Token missing/invalid/expired: end the session and send to sign-in.
        // The 'expired' reason lives in AuthContext so the sign-in page can
        // explain what happened regardless of which redirect lands first.
        logout('expired');
        navigate('/signin', { replace: true });
        return;
      }
      setError(err instanceof Error ? err.message : 'Failed to load accounts.');
      setLoadState('error');
    }
  }, [token, logout, navigate]);

  useEffect(() => {
    void load();
  }, [load]);

  const stats = useMemo(() => {
    const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
    const newToday = accounts.filter((a) => new Date(a.createdAt).getTime() >= dayAgo).length;
    const nationalities = new Set(accounts.map((a) => a.nationality).filter(Boolean)).size;
    return { total: accounts.length, newToday, nationalities };
  }, [accounts]);

  const sorted = useMemo(
    () => [...accounts].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [accounts],
  );

  return (
    <div className="page page-wide">
      <header className="page-header">
        <div>
          <p className="eyebrow">Customer accounts</p>
          <h1 className="page-title">Accounts dashboard</h1>
          <p className="page-subtitle">
            {me ? <>Signed in as <strong>{me.fullName}</strong>. </> : null}
            All onboarded customers, served by the accounts service.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => void load()}
          disabled={loadState === 'loading'}
        >
          <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <path
              d="M13.5 8a5.5 5.5 0 1 1-1.6-3.9M13.5 2.5v2.8h-2.8"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          Refresh
        </button>
      </header>

      <div className="stat-row">
        <div className="stat-card">
          <span className="stat-label">Total accounts</span>
          <span className="stat-value">{loadState === 'loaded' ? stats.total : '—'}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Opened in last 24h</span>
          <span className="stat-value">{loadState === 'loaded' ? stats.newToday : '—'}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Nationalities</span>
          <span className="stat-value">{loadState === 'loaded' ? stats.nationalities : '—'}</span>
        </div>
      </div>

      <section className="card card-flush">
        {loadState === 'loading' && (
          <div className="table-state">
            <div className="spinner" aria-hidden="true" />
            <p>Loading accounts…</p>
          </div>
        )}

        {loadState === 'error' && (
          <div className="table-state">
            <div className="notice notice-danger">
              <div>
                <p className="notice-title">Could not load accounts</p>
                <p className="notice-text">{error}</p>
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          </div>
        )}

        {loadState === 'loaded' && accounts.length === 0 && (
          <div className="table-state">
            <p className="empty-title">No accounts yet</p>
            <p className="empty-text">Accounts appear here as soon as customers complete onboarding.</p>
          </div>
        )}

        {loadState === 'loaded' && accounts.length > 0 && (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Customer</th>
                  <th scope="col">Nationality</th>
                  <th scope="col">Occupation</th>
                  <th scope="col">Annual income</th>
                  <th scope="col">ID number</th>
                  <th scope="col">Created</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((a) => (
                  <tr key={a.id} className={me?.id === a.id ? 'row-self' : undefined}>
                    <td>
                      <div className="customer-cell">
                        <span className="avatar" aria-hidden="true">
                          {initials(a.fullName)}
                        </span>
                        <div className="customer-meta">
                          <span className="customer-name">
                            {a.fullName}
                            {me?.id === a.id && <span className="you-tag">You</span>}
                          </span>
                          <span className="customer-email">{a.email}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="country-chip">
                        {a.nationality && <span className="country-code">{a.nationality}</span>}
                        {countryLabel(a.nationality)}
                      </span>
                    </td>
                    <td>{a.occupation || <span className="muted">—</span>}</td>
                    <td>{annualIncomeLabel(a.annualIncome)}</td>
                    <td className="mono">{a.idNumberMasked || <span className="muted">—</span>}</td>
                    <td className="nowrap">{formatDate(a.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
