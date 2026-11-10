import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Mail, UserPlus, Users } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { listerClients, listerPatients, renvoyerInvitationClient, type Patient } from "@/api/accueil";
import {
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  messageErreur,
  Recherche,
} from "@/features/responsable/commun";
import { DialogueClient } from "./DialogueClient";
import { DialoguePatient } from "./DialoguePatient";

type Onglet = "clients" | "patients";

function age(dateNaissance?: string | null): string | null {
  if (!dateNaissance) return null;
  const naissance = new Date(dateNaissance);
  const maintenant = new Date();
  let ans = maintenant.getFullYear() - naissance.getFullYear();
  const mois = maintenant.getMonth() - naissance.getMonth();
  if (mois < 0 || (mois === 0 && maintenant.getDate() < naissance.getDate())) ans -= 1;
  return `${ans} an${ans > 1 ? "s" : ""}`;
}

export function PageClients() {
  const queryClient = useQueryClient();
  const [onglet, setOnglet] = useState<Onglet>("clients");
  const [recherche, setRecherche] = useState("");
  const [nouveauClient, setNouveauClient] = useState(false);
  const [patientOuvert, setPatientOuvert] = useState(false);
  const [patient, setPatient] = useState<Patient | null>(null);

  const clients = useQuery({ queryKey: ["accueil-clients"], queryFn: listerClients });
  const patients = useQuery({ queryKey: ["accueil-patients"], queryFn: listerPatients });
  const invitation = useMutation({
    mutationFn: renvoyerInvitationClient,
    onSuccess: (client) => toast.success(`Nouveau lien d'activation envoyé à ${client.email}.`),
    onError: (erreur) => toast.error(messageErreur(erreur, "L'invitation n'a pas pu être renvoyée.")),
  });

  const clientsVisibles = (clients.data ?? []).filter((c) => !recherche.trim()
    || contient(c.nomComplet, recherche) || contient(c.email, recherche) || contient(c.telephone, recherche)
    || contient(c.raisonSociale, recherche) || contient(c.ice, recherche));
  const patientsVisibles = (patients.data ?? []).filter((p) => !recherche.trim()
    || contient(`${p.prenom} ${p.nom}`, recherche) || contient(p.code, recherche) || contient(p.cin, recherche)
    || contient(p.telephone, recherche));
  const requete = onglet === "clients" ? clients : patients;

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={Users}
        titre="Clients et patients"
        description="Enregistrez les clients qui se présentent à l'accueil et les patients sur lesquels portent les analyses."
        action={
          <Button icon={<UserPlus className="size-4" aria-hidden="true" />} iconPosition="left"
            onClick={() => (onglet === "clients" ? setNouveauClient(true) : (setPatient(null), setPatientOuvert(true)))}>
            {onglet === "clients" ? "Enregistrer un client" : "Enregistrer un patient"}
          </Button>
        }
      />

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex w-full flex-wrap gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto" role="tablist" aria-label="Clients ou patients">
          {([["clients", "Clients", clients.data?.length], ["patients", "Patients", patients.data?.length]] as const).map(([id, libelle, nombre]) => (
            <button key={id} type="button" role="tab" aria-selected={onglet === id} onClick={() => { setOnglet(id); setRecherche(""); }}
              className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold sm:flex-none ${
                onglet === id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"}`}>
              {libelle}
              <span className={`rounded-full px-2 py-0.5 text-[11px] ${onglet === id ? "bg-white/15" : "bg-surface-alt"}`}>{nombre ?? 0}</span>
            </button>
          ))}
        </div>
        <Recherche valeur={recherche} onChange={setRecherche}
          placeholder={onglet === "clients" ? "Rechercher un nom, un e-mail, un ICE…" : "Rechercher un nom, un code, un CIN…"} />
      </div>

      <EtatChargement
        chargement={requete.isLoading}
        erreur={requete.isError}
        vide={!requete.isLoading && (onglet === "clients" ? clientsVisibles : patientsVisibles).length === 0}
        messageVide={recherche.trim() ? "Aucun résultat." : onglet === "clients" ? "Aucun client enregistré." : "Aucun patient enregistré."}
        onReessayer={() => void requete.refetch()}
      />

      {onglet === "clients" ? (
        <ul className="flex flex-col gap-3">
          {clientsVisibles.map((c) => (
            <li key={c.utilisateurId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{c.raisonSociale ?? c.nomComplet}</p>
                  <Badge size="sm" variant="outline">{c.type === "ENTREPRISE" ? "Entreprise" : "Particulier"}</Badge>
                  {c.compteActive
                    ? <Badge size="sm" variant="mint">Espace activé</Badge>
                    : <Badge size="sm" variant="lime">Activation en attente</Badge>}
                </div>
                <p className="text-xs text-muted-foreground">
                  {c.raisonSociale ? `${c.nomComplet} · ` : ""}{c.email}{c.telephone ? ` · ${c.telephone}` : ""}
                  {c.ice ? ` · ICE ${c.ice}` : ""}
                </p>
                <p className="text-xs text-muted-foreground">Client depuis le {formaterDate(c.datePremierContact)}</p>
              </div>
              {!c.compteActive && (
                <Button size="sm" variant="secondary" icon={<Mail className="size-4" aria-hidden="true" />} iconPosition="left"
                  loading={invitation.isPending && invitation.variables === c.utilisateurId}
                  onClick={() => invitation.mutate(c.utilisateurId)}>
                  Renvoyer l'invitation
                </Button>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <ul className="flex flex-col gap-3">
          {patientsVisibles.map((p) => (
            <li key={p.id} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{p.prenom} {p.nom}</p>
                  <Badge size="sm" variant="outline">{p.code}</Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  {[p.sexe === "F" ? "Féminin" : p.sexe === "M" ? "Masculin" : null,
                    p.dateNaissance ? `né(e) le ${formaterDate(p.dateNaissance)} (${age(p.dateNaissance)})` : null,
                    p.cin ? `CIN ${p.cin}` : null, p.telephone].filter(Boolean).join(" · ") || "—"}
                </p>
              </div>
              <Button size="sm" variant="secondary" onClick={() => { setPatient(p); setPatientOuvert(true); }}>Modifier</Button>
            </li>
          ))}
        </ul>
      )}

      <DialogueClient ouvert={nouveauClient} onFermer={() => setNouveauClient(false)} />
      <DialoguePatient ouvert={patientOuvert} patient={patient}
        onFermer={() => { setPatientOuvert(false); void queryClient.invalidateQueries({ queryKey: ["accueil-patients"] }); }} />
    </Container>
  );
}
