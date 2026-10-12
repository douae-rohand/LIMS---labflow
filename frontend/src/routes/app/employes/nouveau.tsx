import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { EmployeCreationModal } from "@/components/employes/EmployeCreationModal";

export const Route = createFileRoute("/app/employes/nouveau")({
  head: () => ({
    meta: [{ title: "Nouvel employé — LabFlow LIMS" }],
  }),
  component: NouvelEmployeRedirectPage,
});

function NouvelEmployeRedirectPage() {
  const navigate = useNavigate();

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      void navigate({
        to: "/app/employes",
        search: { page: 1, tri: "nom", direction: "asc" },
        replace: true,
      });
    }
  };

  const handleSuccessCreated = (employeId: number | undefined) => {
    void navigate({
      to: "/app/employes",
      search: {
        page: 1,
        tri: "nom",
        direction: "asc",
        ...(employeId ? { employe: employeId } : {}),
      },
      replace: true,
    });
  };

  return (
    <EmployeCreationModal
      open={true}
      onOpenChange={handleOpenChange}
      onSuccessCreated={handleSuccessCreated}
    />
  );
}
