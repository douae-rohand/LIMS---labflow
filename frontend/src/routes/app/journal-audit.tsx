import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { JournalAudit } from "@/features/super-admin/audit/JournalAudit";

export const Route = createFileRoute("/app/journal-audit")({
  beforeLoad: () => {
    if (getSession()?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  head: () => ({
    meta: [{ title: "Journal d'audit — LabFlow" }],
  }),
  component: PageJournal,
});

function PageJournal() {
  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-5">
        <h1 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">Journal d'audit</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Consultation seule. Chaque ligne indique qui a fait quoi, quand, et les valeurs avant et après.
        </p>
      </header>
      <JournalAudit />
    </div>
  );
}
