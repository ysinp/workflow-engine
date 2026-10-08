import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, listProducts, startOnboarding, submitProductSelectionStep } from '../api/client';
import type { ProductCategory, ProductResponse } from '../types';

type Category = 'ALL' | ProductCategory;

const CATEGORIES: Category[] = ['ALL', 'PERSONAL', 'BUSINESS'];

/**
 * Presentational only: every product below leads into the exact same KYC wizard and
 * account-creation flow (there is only one provisioning path in this system - a checking-style
 * account with a demo balance, gated by the risk workflow). The chosen product is carried
 * through as `location.state.product` purely for display in the wizard and the final
 * confirmation screen; it drives no backend logic.
 *
 * Product Selection is itself the OnboardingWizardWorkflow's first step: the workflow is started
 * the moment this screen mounts (not on "Apply"), so it is already running by the time the
 * customer is looking at it. Clicking "Apply" submits the chosen product as an Update against
 * that running execution and advances to the next step.
 *
 * Switching the ALL/PERSONAL/BUSINESS tab re-queries product-service with ?category=... rather
 * than filtering an already-fetched list client-side.
 */
export default function ProductSelect() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<Category>('ALL');
  const [products, setProducts] = useState<ProductResponse[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selected, setSelected] = useState<ProductResponse | null>(null);
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);

  const [workflowId, setWorkflowId] = useState<string | null>(null);
  const [workflowError, setWorkflowError] = useState<string | null>(null);
  const workflowStartedRef = useRef(false);

  const beginWorkflow = () => {
    workflowStartedRef.current = true;
    setWorkflowError(null);
    startOnboarding()
      .then((started) => setWorkflowId(started.workflowId))
      .catch((err) => {
        workflowStartedRef.current = false;
        setWorkflowError(err instanceof ApiError ? err.message : 'We could not start your application.');
      });
  };

  // Starts the workflow exactly once per visit to this screen, regardless of render/effect
  // re-runs (e.g. React StrictMode's double-invoke in development).
  useEffect(() => {
    if (!workflowStartedRef.current) beginWorkflow();
  }, []);

  useEffect(() => {
    let cancelled = false;
    setProducts(null);
    setLoadError(null);
    listProducts(filter === 'ALL' ? undefined : filter)
      .then((result) => {
        if (cancelled) return;
        setProducts(result);
      })
      .catch((err) => {
        if (cancelled) return;
        setLoadError(err instanceof ApiError ? err.message : 'We could not load the product catalog.');
      });
    return () => {
      cancelled = true;
    };
  }, [filter]);

  const changeFilter = (next: Category) => {
    if (next === filter) return;
    setFilter(next);
    setSelected(null);
  };

  const handleGetStarted = async () => {
    if (!selected || !workflowId || starting) return;
    setStarting(true);
    setStartError(null);
    try {
      const result = await submitProductSelectionStep(workflowId, selected.name);
      navigate(`/signup/create-new/${result.nextActivity}`, {
        state: { product: selected.name, workflowId },
      });
    } catch (err) {
      setStarting(false);
      setStartError(
        err instanceof ApiError ? err.message : 'We could not start your application. Please try again.',
      );
    }
  };

  return (
    <div className="page page-narrow">
      <h1 className="page-title">Let's get started</h1>
      <p className="page-subtitle">Select a product.</p>

      <div className="product-tabs" role="tablist" aria-label="Product category">
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            type="button"
            role="tab"
            aria-selected={filter === cat}
            className={`product-tab${filter === cat ? ' product-tab-active' : ''}`}
            onClick={() => changeFilter(cat)}
          >
            {cat}
          </button>
        ))}
      </div>

      {workflowError && (
        <div className="notice notice-danger" role="alert">
          <div>
            <p className="notice-title">Couldn't start your application</p>
            <p className="notice-text">{workflowError}</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={beginWorkflow}>
              Retry
            </button>
          </div>
        </div>
      )}

      {loadError && (
        <div className="notice notice-danger" role="alert">
          <div>
            <p className="notice-title">Couldn't load products</p>
            <p className="notice-text">{loadError}</p>
          </div>
        </div>
      )}

      {!products && !loadError && (
        <div className="notice notice-info">
          <div className="spinner" aria-hidden="true" />
          <p className="notice-text">Loading products…</p>
        </div>
      )}

      {products && (
        <ul className="product-list">
          {products.map((product) => {
            const isSelected = selected?.id === product.id;
            return (
              <li key={product.id}>
                <button
                  type="button"
                  className={`product-row${isSelected ? ' product-row-selected' : ''}`}
                  aria-pressed={isSelected}
                  onClick={() => setSelected(product)}
                >
                  <span className="product-name">{product.name}</span>
                  <span className="product-description">{product.description}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {startError && (
        <div className="notice notice-danger" role="alert">
          <div>
            <p className="notice-title">Something went wrong</p>
            <p className="notice-text">{startError}</p>
          </div>
        </div>
      )}

      <div className="product-select-footer">
        <button
          type="button"
          className="btn btn-primary btn-lg"
          disabled={!selected || !workflowId || starting}
          onClick={() => void handleGetStarted()}
        >
          {starting && <span className="spinner spinner-inverse" aria-hidden="true" />}
          {starting ? 'Starting…' : 'Apply'}
        </button>
      </div>
    </div>
  );
}
