import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError, signIn } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Field from '../components/Field';

interface SignInLocationState {
  email?: string;
  from?: string;
  reason?: 'expired';
  /** Arbitrary state a protected route was carrying (e.g. { submissionId }) - see ProtectedRoute. */
  forwardState?: unknown;
}

/** Where an account lands after sign-in when there's no (role-appropriate) `from` to return to. */
function defaultLandingFor(role: 'CUSTOMER' | 'REVIEWER'): string {
  return role === 'REVIEWER' ? '/dashboard' : '/account';
}

export default function SignIn() {
  const { login, isAuthenticated, account, logoutReason } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as SignInLocationState;
  const sessionExpired = logoutReason === 'expired' || state.reason === 'expired';

  const [email, setEmail] = useState(state.email ?? '');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated && account) {
    return <Navigate to={defaultLandingFor(account.role)} replace />;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Enter your email and password.');
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const { token, account: signedInAccount } = await signIn(email.trim(), password);
      login(token, signedInAccount);
      // Only honor `from` when it's a page this role can actually see - a customer bounced off
      // /review or /dashboard shouldn't be sent straight back into another access-denied screen.
      const canReturnTo =
        state.from &&
        (signedInAccount.role === 'REVIEWER' || (state.from !== '/review' && state.from !== '/dashboard'));
      const target = canReturnTo ? state.from! : defaultLandingFor(signedInAccount.role);
      navigate(target, { replace: true, state: state.forwardState });
    } catch (err) {
      setSubmitting(false);
      if (err instanceof ApiError && (err.status === 401 || err.code === 'INVALID_CREDENTIALS')) {
        setError('Invalid email or password.');
      } else {
        setError('We could not reach the accounts service. Please try again.');
      }
    }
  };

  return (
    <div className="page auth-page">
      <section className="card auth-card">
        <div className="auth-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22">
            <rect x="5" y="10.5" width="14" height="9.5" rx="2.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
            <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
          </svg>
        </div>
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Sign in to your customer account, or as a compliance reviewer.</p>

        {sessionExpired && !error && (
          <div className="notice notice-warning">
            <div>
              <p className="notice-title">Your session has expired</p>
              <p className="notice-text">Please sign in again to continue.</p>
            </div>
          </div>
        )}
        {state.email && !sessionExpired && !error && (
          <div className="notice notice-success">
            <div>
              <p className="notice-title">Your account is ready</p>
              <p className="notice-text">Sign in with the password you just created.</p>
            </div>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <Field id="signin-email" label="Email address">
            <input
              id="signin-email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={submitting}
              placeholder="jane.doe@example.com"
              aria-invalid={error ? true : undefined}
            />
          </Field>
          <Field id="signin-password" label="Password">
            <input
              id="signin-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={submitting}
              aria-invalid={error ? true : undefined}
            />
          </Field>

          {error && (
            <div className="notice notice-danger" role="alert">
              <p className="notice-text notice-text-strong">{error}</p>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={submitting}>
            {submitting && <span className="spinner spinner-inverse" aria-hidden="true" />}
            {submitting ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p className="auth-footer">
          New customer? <Link to="/signup/create-new">Open an account</Link>
        </p>
        <p className="auth-footer auth-footer-muted">
          Demo: compliance reviewer login is <code>reviewer@demo.local</code> / <code>ReviewDemo123!</code>
        </p>
      </section>
    </div>
  );
}
