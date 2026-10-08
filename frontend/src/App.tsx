import { useEffect } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import NavBar from './components/NavBar';
import AccountOverview from './pages/AccountOverview';
import Dashboard from './pages/Dashboard';
import ProductSelect from './pages/ProductSelect';
import ReviewConsole from './pages/ReviewConsole';
import SignIn from './pages/SignIn';
import SignUp from './pages/SignUp';
import WorkflowEditor from './pages/WorkflowEditor';

/** Reset scroll position on route change (e.g. long sign-up result -> sign-in). */
function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

export default function App() {
  return (
    <AuthProvider>
      <ScrollToTop />
      <div className="app-shell">
        <NavBar />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<SignIn />} />
            <Route path="/signin" element={<SignIn />} />
            <Route path="/signup/create-new" element={<ProductSelect />} />
            <Route path="/signup/create-new/:step" element={<SignUp />} />
            <Route
              path="/account"
              element={
                <ProtectedRoute>
                  <AccountOverview />
                </ProtectedRoute>
              }
            />
            <Route
              path="/review"
              element={
                <ProtectedRoute requireRole="REVIEWER">
                  <ReviewConsole />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute requireRole="REVIEWER">
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/workflow-editor"
              element={
                <ProtectedRoute requireRole="REVIEWER">
                  <WorkflowEditor />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <footer className="app-footer">
          Demo environment · Account creation and risk assessment are orchestrated by independent services.
        </footer>
      </div>
    </AuthProvider>
  );
}
