import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { AccountRole } from '../types';
import { useAuth } from './AuthContext';

interface ProtectedRouteProps {
  children: ReactNode;
  /** When set, the signed-in account's role must match, not just "authenticated". */
  requireRole?: AccountRole;
}

/**
 * Redirects to /signin when there is no authenticated session. When it also requires a specific
 * role and the signed-in account doesn't have it, it renders an in-place "access denied" message
 * instead of redirecting - that's clearer than silently bouncing an already-signed-in customer.
 */
export default function ProtectedRoute({ children, requireRole }: ProtectedRouteProps) {
  const { isAuthenticated, account } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // location.state is preserved separately from the flat `from`/`email`/`reason` fields
    // SignIn reads, so a deep link like "Open Review Console" (which attaches its own
    // { submissionId } state) survives the sign-in round trip.
    return (
      <Navigate
        to="/signin"
        replace
        state={{ from: location.pathname, forwardState: location.state }}
      />
    );
  }

  if (requireRole && account?.role !== requireRole) {
    return (
      <div className="page page-narrow">
        <section className="card">
          <p className="eyebrow">Access restricted</p>
          <h1>This page is for compliance reviewers only</h1>
          <p className="lead">
            Signed in as <strong>{account?.email}</strong>, which does not have reviewer access.
            Sign in with a compliance-reviewer account to continue.
          </p>
        </section>
      </div>
    );
  }

  return <>{children}</>;
}
