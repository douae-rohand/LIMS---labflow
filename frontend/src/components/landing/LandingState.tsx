import type { ReactNode } from "react";
import { Button, Skeleton } from "@/components/lab";

type LandingStateProps = {
  loading: boolean;
  error: Error | null;
  onRetry: () => void;
  empty?: boolean;
  emptyMessage?: string;
  skeleton: ReactNode;
  children: ReactNode;
};

export function LandingState({
  loading,
  error,
  onRetry,
  empty = false,
  emptyMessage = "Aucune donnée disponible.",
  skeleton,
  children,
}: LandingStateProps) {
  if (loading) {
    return <div aria-busy="true">{skeleton}</div>;
  }

  if (error) {
    return (
      <div
        role="alert"
        className="flex flex-col items-start gap-3 rounded-3xl border border-brand-900/10 bg-surface-card px-5 py-6"
      >
        <p className="text-sm font-semibold text-ink-900">
          Impossible de charger les données depuis le serveur.
        </p>
        <p className="text-sm text-muted-foreground">{error.message}</p>
        <Button size="sm" variant="secondary" onClick={onRetry}>
          Réessayer
        </Button>
      </div>
    );
  }

  if (empty) {
    return (
      <p className="rounded-3xl bg-surface-card px-5 py-6 text-sm text-muted-foreground">
        {emptyMessage}
      </p>
    );
  }

  return <>{children}</>;
}

export function StatsSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
      {Array.from({ length: 3 }, (_, i) => (
        <Skeleton key={i} className="h-36 w-full" />
      ))}
    </div>
  );
}

export function CardsSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: count }, (_, i) => (
        <Skeleton key={i} className="h-40 w-full" />
      ))}
    </div>
  );
}
