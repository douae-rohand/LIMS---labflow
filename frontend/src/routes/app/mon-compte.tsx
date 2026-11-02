import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { MonCompte } from "@/features/super-admin/compte/MonCompte";

export const Route = createFileRoute("/app/mon-compte")({
  beforeLoad: () => {
    if (getSession()?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  head: () => ({
    meta: [{ title: "Mon compte — LabFlow" }],
  }),
  component: PageCompte,
});

function PageCompte() {
  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-5">
        <h1 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">Mon compte</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Profil, mot de passe, double authentification facultative et sessions de renouvellement.
        </p>
      </header>
      <MonCompte />
    </div>
  );
}
