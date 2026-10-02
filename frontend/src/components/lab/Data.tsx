import { cn } from "@/lib/utils";

export function Avatar({
  initials,
  src,
  alt,
  className,
}: {
  initials?: string;
  src?: string;
  alt?: string;
  className?: string | undefined;
}) {
  return (
    <span
      className={cn(
        "inline-flex size-11 items-center justify-center overflow-hidden rounded-full bg-mint-200 text-sm font-bold text-brand-900",
        className,
      )}
    >
      {src ? (
        <img
          src={src}
          alt={alt ?? ""}
          loading="lazy"
          className="size-full object-cover"
        />
      ) : (
        initials
      )}
    </span>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return (
    <span
      className={cn("block animate-pulse rounded-xl bg-surface-card", className)}
      aria-hidden="true"
    />
  );
}

export function Table({
  columns,
  rows,
}: {
  columns: string[];
  rows: (string | number)[][];
}) {
  return (
    <div className="overflow-hidden rounded-3xl border border-brand-900/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            {columns.map((c) => (
              <th key={c} className="px-4 py-3 font-semibold">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} className="border-t border-brand-900/10">
              {row.map((cell, j) => (
                <td key={j} className="px-4 py-3 text-muted-foreground">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
