export function EtatVide({ titre, detail }: { titre: string; detail?: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-brand-900/15 bg-card px-4 py-10 text-center">
      <p className="text-sm font-semibold text-ink-900">{titre}</p>
      {detail && <p className="mx-auto mt-1 max-w-md text-sm text-muted-foreground">{detail}</p>}
    </div>
  );
}
