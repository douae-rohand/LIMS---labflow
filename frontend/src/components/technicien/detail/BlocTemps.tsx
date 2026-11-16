import { useState } from "react";
import { Clock } from "lucide-react";
import { Button, Card, Input, Label } from "@/components/lab";
import { formaterDuree } from "@/components/technicien/libelles";
import type { TempsPayload } from "@/api/technicien/types";
import { useDeclarerTemps } from "@/hooks/useTechnicienEssais";

const DUREE_MAX = 480;
const RAPIDES = [15, 30, 60];

/** Bloc temps passé : saisie de la durée en minutes + boutons rapides. */
export function BlocTemps({ id, dureeMinutes }: { id: number; dureeMinutes: number | null }) {
  const [valeur, setValeur] = useState<string>(dureeMinutes !== null ? String(dureeMinutes) : "");
  const mutation = useDeclarerTemps();
  const enCours = mutation.isPending;

  const ajouter = (minutes: number) => {
    const base = Number(valeur) || 0;
    const total = Math.min(base + minutes, DUREE_MAX);
    setValeur(String(total));
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const duree = Number(valeur);
    if (!Number.isFinite(duree) || duree <= 0) return;
    const payload: TempsPayload = { dureeMinutes: Math.min(Math.round(duree), DUREE_MAX) };
    mutation.mutate({ id, payload }, { onSuccess: () => setValeur("") });
  };

  return (
    <Card variant="flat" padding="md" className="space-y-4">
      <div className="flex items-center gap-2">
        <Clock className="size-5 text-brand-900" aria-hidden="true" />
        <h2 className="text-lg font-bold text-ink-900">Temps passé</h2>
      </div>

      {dureeMinutes !== null ? (
        <p className="text-sm text-muted-foreground">
          Durée enregistrée :{" "}
          <span className="font-semibold text-ink-900">{formaterDuree(dureeMinutes)}</span>
        </p>
      ) : (
      <form className="space-y-3" onSubmit={onSubmit}>
        <div className="space-y-2">
          <Label htmlFor="duree-minutes">Durée en minutes</Label>
          <div className="flex items-center gap-2">
            <Input
              id="duree-minutes"
              type="number"
              inputMode="numeric"
              min={1}
              max={DUREE_MAX}
              value={valeur}
              onChange={(e) => setValeur(e.target.value)}
              disabled={enCours}
              className="w-28 text-base"
            />
            <div className="flex flex-wrap gap-2">
              {RAPIDES.map((m) => (
                <Button
                  key={m}
                  type="button"
                  variant="secondary"
                  size="sm"
                  disabled={enCours}
                  onClick={() => ajouter(m)}
                >
                  +{m}
                </Button>
              ))}
            </div>
          </div>
          <p className="text-xs text-muted-foreground">Plafond : {DUREE_MAX} minutes (8 h).</p>
        </div>
        <Button type="submit" loading={enCours} disabled={enCours || valeur === ""}>
          Enregistrer le temps
        </Button>
      </form>
      )}
    </Card>
  );
}
