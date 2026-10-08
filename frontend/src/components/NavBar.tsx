import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-link active' : 'nav-link');

export default function NavBar() {
  const { isAuthenticated, account, logout } = useAuth();
  const navigate = useNavigate();
  const isReviewer = account?.role === 'REVIEWER';

  const handleSignOut = () => {
    logout(); // user-initiated: no 'expired' reason
    navigate('/signin', { replace: true });
  };

  const brandDestination = !isAuthenticated ? '/' : isReviewer ? '/dashboard' : '/account';

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to={brandDestination} className="brand">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path
                d="M12 2.8l7.5 3v5.6c0 4.6-3.1 8.4-7.5 9.8-4.4-1.4-7.5-5.2-7.5-9.8V5.8z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinejoin="round"
              />
              <path
                d="M8.6 12.2l2.3 2.3 4.5-4.8"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <span className="brand-text">
            <span className="brand-name">Secure Banking</span>
          </span>
        </Link>

        <nav className="navbar-links" aria-label="Main">
          {isAuthenticated ? (
            <>
              {isReviewer ? (
                <>
                  <NavLink to="/dashboard" className={linkClass}>
                    Dashboard
                  </NavLink>
                  <NavLink to="/review" className={linkClass}>
                    Review Console
                  </NavLink>
                  <NavLink to="/workflow-editor" className={linkClass}>
                    Workflow Editor
                  </NavLink>
                </>
              ) : (
                <NavLink to="/account" className={linkClass}>
                  My Account
                </NavLink>
              )}
              {account && (
                <span className="nav-user" title={account.email}>
                  <span className="nav-user-avatar" aria-hidden="true">
                    {(account.fullName.trim()[0] ?? '?').toUpperCase()}
                  </span>
                  <span className="nav-user-text">
                    <span className="nav-user-name">{account.fullName}</span>
                    <span className="nav-user-email">{account.email}</span>
                  </span>
                </span>
              )}
              <button type="button" className="btn btn-secondary btn-sm" onClick={handleSignOut}>
                Sign Out
              </button>
            </>
          ) : (
            <NavLink to="/signin" className={linkClass} end>
              Sign In
            </NavLink>
          )}
        </nav>
      </div>
    </header>
  );
}
