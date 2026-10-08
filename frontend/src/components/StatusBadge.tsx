interface StatusBadgeProps {
  status: string;
  label?: string;
}

const STATUS_CLASS: Record<string, string> = {
  RUNNING: 'badge badge-info',
  AWAITING_REVIEW: 'badge badge-warning',
  COMPLETED: 'badge badge-success',
  FAILED: 'badge badge-danger',
  APPROVED: 'badge badge-success',
  REJECTED: 'badge badge-warning',
  LOW: 'badge badge-success',
  HIGH: 'badge badge-warning',
};

export default function StatusBadge({ status, label }: StatusBadgeProps) {
  const className = STATUS_CLASS[status] ?? 'badge';
  return (
    <span className={className}>
      <span className="badge-dot" aria-hidden="true" />
      {label ?? status}
    </span>
  );
}
