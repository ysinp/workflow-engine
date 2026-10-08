interface StepProgressProps {
  steps: string[];
  current: number; // zero-based
  onStepClick?: (index: number) => void;
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
      <path
        d="M3.5 8.5l3 3 6-7"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/**
 * Numbered step indicator: circles joined by a connector line. Completed steps
 * show a check and are clickable (to jump back); the current step is
 * highlighted; upcoming steps are muted and not clickable.
 */
export default function StepProgress({ steps, current, onStepClick }: StepProgressProps) {
  return (
    <ol className="stepper" aria-label="Application progress">
      {steps.map((label, index) => {
        const state = index < current ? 'complete' : index === current ? 'current' : 'upcoming';
        const clickable = state === 'complete' && Boolean(onStepClick);
        return (
          <li
            key={label}
            className={`stepper-item stepper-item-${state}`}
            aria-current={state === 'current' ? 'step' : undefined}
          >
            <button
              type="button"
              className="stepper-marker"
              onClick={clickable ? () => onStepClick?.(index) : undefined}
              disabled={!clickable}
              aria-label={`Step ${index + 1}: ${label}${state === 'complete' ? ' (completed)' : ''}`}
            >
              {state === 'complete' ? <CheckIcon /> : index + 1}
            </button>
            <span className="stepper-label">{label}</span>
          </li>
        );
      })}
    </ol>
  );
}
