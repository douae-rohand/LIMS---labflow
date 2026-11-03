import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { JournalAudit } from "@/features/super-admin/audit/JournalAudit";
import { EnTetePage } from "@/features/super-admin/EnTetePage";

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
      <EnTetePage
        titre="Journal d'audit"
        description="Consultation seule. Chaque ligne indique qui a fait quoi, quand, et les valeurs avant et après."
      />
      <JournalAudit />
    </div>
  );
}
