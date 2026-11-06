import { useMutation } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as exports from "@/api/exports";

export function useExports() {
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  const exporter = useMutation({
    mutationFn: ({ jeu, format }: { jeu: exports.ExportJeu; format: exports.ExportFormat }) =>
      exports.exporter(jeu, format),
    onSuccess: (blob, { jeu, format }) => {
      const nomFichier = exports.genererNomFichier(jeu, format);
      exports.telecharger(blob, nomFichier);
    },
  });

  return { exporter, enabled };
}
