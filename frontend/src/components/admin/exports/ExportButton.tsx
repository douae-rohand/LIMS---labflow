import { Download } from "lucide-react";
import { Button } from "@/components/lab";
import { useSession } from "@/api/session";
import { PERMISSION_EXPORT, type ExportJeu } from "@/api/exports";
import { useExports } from "@/hooks/useExports";

interface ExportButtonProps {
  jeu: ExportJeu;
  disabled?: boolean;
}

export function ExportButton({ jeu, disabled }: ExportButtonProps) {
  const { session } = useSession();
  const { exporter } = useExports();
  if (!session?.user.permissions.includes(PERMISSION_EXPORT[jeu])) return null;

  return (
    <Button
      variant="primary"
      size="sm"
      icon={<Download className="size-4" aria-hidden="true" />}
      iconPosition="left"
      onClick={() => exporter.mutate({ jeu, format: "csv" })}
      disabled={disabled || exporter.isPending}
    >
      {exporter.isPending ? "Export..." : "Exporter (CSV)"}
    </Button>
  );
}
