import { useAuth } from '../auth/AuthContext';
import { annualIncomeLabel, countryLabel } from '../constants/kyc';

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

function formatBalance(balance: number | undefined): string {
  if (balance === undefined) return '—';
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(balance);
}

/**
 * A customer's own account details, shown right after sign-in. Reads straight from the signed-in
 * session (already carries the full AccountResponse from sign-in) rather than calling the
 * accounts API - that endpoint is reviewer-only (see create-account's SecurityConfig), and a
 * customer viewing their own data doesn't need a network round trip anyway.
 */
export default function AccountOverview() {
  const { account } = useAuth();

  if (!account) return null;

  return (
    <div className="page page-narrow">
      <section className="card">
        <p className="eyebrow">My account</p>
        <h1 className="page-title">Welcome, {account.fullName}</h1>
        <p className="lead">Here's a summary of your account.</p>

        <div className="balance-panel">
          <span className="balance-label">Account balance</span>
          <span className="balance-value">{formatBalance(account.balance)}</span>
        </div>

        <dl className="summary-grid">
          <div>
            <dt>Email</dt>
            <dd>{account.email}</dd>
          </div>
          <div>
            <dt>Member since</dt>
            <dd>{formatDate(account.createdAt)}</dd>
          </div>
          <div>
            <dt>Nationality</dt>
            <dd>{countryLabel(account.nationality)}</dd>
          </div>
          <div>
            <dt>Occupation</dt>
            <dd>{account.occupation || '—'}</dd>
          </div>
          <div>
            <dt>Annual income</dt>
            <dd>{annualIncomeLabel(account.annualIncome)}</dd>
          </div>
          <div>
            <dt>ID number</dt>
            <dd className="mono">{account.idNumberMasked || '—'}</dd>
          </div>
        </dl>
      </section>
    </div>
  );
}
