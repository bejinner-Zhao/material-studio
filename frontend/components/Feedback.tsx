interface ErrorBlockProps {
  message: string;
  onRetry?: () => void;
}

export function ErrorBlock({ message, onRetry }: ErrorBlockProps) {
  return (
    <div
      role="alert"
      className="flex flex-col items-center gap-3 rounded-xl border border-danger/30 bg-danger/5 px-6 py-12 text-center"
    >
      <p className="text-sm text-white/80">{message}</p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-lg border border-line bg-panel px-3 py-1.5 text-sm text-white/80 hover:border-white/30"
        >
          重试
        </button>
      ) : null}
    </div>
  );
}

export function SkeletonGrid({ count = 8 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="aspect-[4/5] animate-pulse rounded-card bg-panel" />
      ))}
    </div>
  );
}

export function EmptyBlock({ message }: { message: string }) {
  return (
    <p className="rounded-xl border border-dashed border-line px-4 py-12 text-center text-sm text-muted">
      {message}
    </p>
  );
}
