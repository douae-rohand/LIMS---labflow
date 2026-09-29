import { Activity, AlertTriangle, Bell, BookOpen, Boxes, Building2, CalendarClock, ClipboardList, Crown, FileCheck2, FileSignature, FlaskConical, Gauge, KeyRound, LayoutDashboard, ListChecks, Mail, MessageSquare, Microscope, Receipt, ScrollText, Send, ShieldCheck, Smartphone, Sparkles, Star, TestTubes, Truck, UserCog, UserRound, Users, Webhook, Workflow, } from "lucide-react";
import { photos } from "./images";
export const hero = {
    badge: "Plateforme LIMS multi-laboratoires",
    title: "Digitalisez tout le cycle d'analyse de votre laboratoire",
    subtitle: "De la demande client au rapport validé : planification, échantillons, résultats, validation à deux niveaux et traçabilité complète des actions sensibles.",
    primaryCta: "Intégrer mon laboratoire",
    secondaryCta: "Découvrir le workflow",
    bento: [
        {
            icon: ScrollText,
            title: "Traçabilité complète",
            description: "Chaque action sensible est horodatée et attribuée.",
        },
        {
            icon: FileCheck2,
            title: "Conformité ISO/IEC 17025",
            description: "Modèles de rapports et contrôles qualité intégrés.",
        },
        {
            icon: Building2,
            title: "Multi-tenant",
            description: "Un schéma de données dédié par laboratoire.",
        },
    ],
};
export const stats = [
    { value: 6, label: "rôles utilisateurs", icon: Users },
    { value: 9, label: "étapes de workflow", icon: Workflow },
    { value: 14, label: "modules métier", icon: Boxes },
    {
        value: 100,
        suffix: " %",
        label: "de traçabilité des actions sensibles",
        icon: ShieldCheck,
    },
];
export const features = [
    {
        icon: ClipboardList,
        title: "Gestion des demandes",
        description: "Réception, qualification et décision sur chaque demande client, avec historique complet.",
    },
    {
        icon: TestTubes,
        title: "Catalogue d'essais paramétrable",
        description: "Méthodes, unités, seuils et tarifs configurables par laboratoire.",
    },
    {
        icon: CalendarClock,
        title: "Planification et attribution",
        description: "Affectation des essais aux techniciens selon charge et compétences.",
    },
    {
        icon: Truck,
        title: "Réception des échantillons",
        description: "Identification unique, étiquetage et suivi des conditions de conservation.",
    },
    {
        icon: ListChecks,
        title: "Saisie et contrôle des résultats",
        description: "Contrôles de cohérence, valeurs hors seuils signalées automatiquement.",
    },
    {
        icon: Receipt,
        title: "Facturation",
        description: "Devis, factures et suivi des règlements alignés sur le catalogue d'essais.",
    },
    {
        icon: Boxes,
        title: "Stock et réactifs",
        description: "Lots, péremptions et alertes de réapprovisionnement.",
    },
    {
        icon: Star,
        title: "Enquêtes de satisfaction",
        description: "Questionnaire envoyé à la clôture de chaque demande.",
    },
    {
        icon: LayoutDashboard,
        title: "Tableaux de bord par rôle",
        description: "Indicateurs de délais, de charge et de qualité en temps réel.",
    },
];
export const workflowSteps = [
    {
        title: "Enregistrement et décision",
        description: "La demande est qualifiée puis acceptée ou refusée.",
        icon: ClipboardList,
    },
    {
        title: "Attribution et planification",
        description: "Les essais sont affectés et planifiés.",
        icon: CalendarClock,
    },
    {
        title: "Réception des échantillons",
        description: "Identification et contrôle de conformité.",
        icon: Truck,
    },
    {
        title: "Réalisation des essais",
        description: "Exécution selon la méthode du catalogue.",
        icon: Microscope,
    },
    {
        title: "Saisie des résultats",
        description: "Valeurs, incertitudes et pièces jointes.",
        icon: ListChecks,
    },
    {
        title: "Validation technique",
        description: "Contrôle par un pair avant transmission.",
        icon: FileCheck2,
    },
    {
        title: "Validation responsable",
        description: "Signature électronique du responsable.",
        icon: FileSignature,
    },
    {
        title: "Génération et diffusion du rapport",
        description: "Rapport PDF signé, diffusé au client.",
        icon: Send,
    },
    {
        title: "Enquête et clôture",
        description: "Satisfaction recueillie puis dossier clôturé.",
        icon: Star,
    },
];
export const requestStatuses = [
    "NOUVEAU",
    "ACCEPTÉE",
    "EN_COURS",
    "EN_VALIDATION",
    "TERMINÉE",
    "ANNULÉE",
    "REFUSÉE",
];
export const roles = [
    {
        role: "Client",
        icon: UserRound,
        capabilities: [
            "Déposer une demande d'analyse",
            "Suivre l'avancement en temps réel",
            "Télécharger les rapports validés",
        ],
    },
    {
        role: "Personnel d'accueil",
        icon: ClipboardList,
        capabilities: [
            "Enregistrer les demandes",
            "Réceptionner et identifier les échantillons",
            "Éditer les devis et factures",
        ],
    },
    {
        role: "Technicien",
        icon: Microscope,
        capabilities: [
            "Consulter les essais attribués",
            "Saisir les résultats et incertitudes",
            "Signaler les non-conformités",
        ],
    },
    {
        role: "Responsable de laboratoire",
        icon: FileSignature,
        capabilities: [
            "Valider techniquement les résultats",
            "Signer électroniquement les rapports",
            "Piloter la charge et les délais",
        ],
        twoFactor: true,
    },
    {
        role: "Administrateur",
        icon: UserCog,
        capabilities: [
            "Gérer utilisateurs et permissions",
            "Paramétrer le catalogue d'essais",
            "Consulter le journal d'audit",
        ],
        twoFactor: true,
    },
    {
        role: "Super Administrateur",
        icon: Crown,
        capabilities: [
            "Créer et isoler les laboratoires",
            "Superviser la plateforme",
            "Définir les politiques de sécurité",
        ],
    },
];
export const validation = {
    eyebrow: "Validation",
    title: "Validation à deux niveaux et signature électronique",
    description: "Aucun rapport ne sort du laboratoire sans double contrôle. Le validateur peut valider, retourner pour correction ou rejeter, et chaque décision alimente un journal d'audit immuable.",
    points: [
        "Validation technique par un pair, puis validation du responsable",
        "Signature électronique nominative avec horodatage",
        "Retour pour correction commenté, sans perte d'historique",
    ],
    floating: [
        { icon: ScrollText, label: "Journal d'audit immuable" },
        { icon: ShieldCheck, label: "Rapport bloqué sans signature" },
    ],
    photo: photos.technicianMicroscope,
};
export const ai = {
    eyebrow: "Intelligence artificielle",
    title: "Une aide à l'analyse, jamais un remplaçant",
    description: "Les modèles proposent, les experts décident. Chaque suggestion reste soumise à la validation humaine.",
    cards: [
        {
            icon: AlertTriangle,
            title: "Détection d'anomalies",
            description: "Score d'anomalie calculé sur l'historique des essais et les seuils de la méthode.",
        },
        {
            icon: MessageSquare,
            title: "Assistant conversationnel RAG",
            description: "Réponses sourcées dans les procédures et modes opératoires du laboratoire.",
        },
        {
            icon: Sparkles,
            title: "Synthèse automatique",
            description: "Proposition de commentaire de rapport, relue et validée par le responsable.",
        },
    ],
    notice: "La validation reste humaine : aucune suggestion n'est diffusée sans signature d'un responsable.",
};
export const automation = {
    eyebrow: "Automatisation",
    title: "Des événements métier aux notifications",
    description: "Chaque changement d'état déclenche un webhook et un workflow, puis notifie les bonnes personnes sur le bon canal.",
    chain: [
        { icon: Activity, title: "Événement", description: "Statut, résultat, seuil" },
        { icon: Webhook, title: "Webhook", description: "Appel signé et rejouable" },
        { icon: Workflow, title: "Workflow", description: "Règles et destinataires" },
    ],
    channels: [
        { icon: Mail, label: "Email" },
        { icon: Bell, label: "In-app" },
        { icon: Smartphone, label: "Push" },
    ],
};
export const security = {
    eyebrow: "Sécurité",
    title: "Multi-tenant, cloisonné et auditable",
    description: "Chaque laboratoire dispose de son schéma de données, de ses rôles et de son journal d'audit.",
    cards: [
        {
            icon: Building2,
            title: "Isolation par laboratoire",
            description: "Un schéma dédié par tenant, aucun croisement de données.",
        },
        {
            icon: KeyRound,
            title: "Authentification JWT + 2FA",
            description: "Second facteur obligatoire pour les rôles sensibles.",
        },
        {
            icon: UserCog,
            title: "Permissions granulaires",
            description: "Droits par rôle, par module et par action.",
        },
        {
            icon: ScrollText,
            title: "Journal d'audit",
            description: "Traçabilité horodatée et non modifiable.",
        },
        {
            icon: ShieldCheck,
            title: "Conformité loi 09-08 (CNDP)",
            description: "Traitement des données personnelles encadré.",
        },
        {
            icon: FileCheck2,
            title: "ISO/IEC 17025",
            description: "Exigences documentaires et métrologiques couvertes.",
        },
    ],
    photo: photos.labCorridor,
};
export const domains = [
    {
        number: "01",
        title: "Médical",
        icon: Activity,
        examples: ["Hématologie", "Biochimie", "Sérologie"],
        photo: photos.sampling,
    },
    {
        number: "02",
        title: "Industriel",
        icon: Gauge,
        examples: ["Matériaux", "Contrôle de lots", "Métrologie"],
        photo: photos.teamLab,
    },
    {
        number: "03",
        title: "Environnemental",
        icon: FlaskConical,
        examples: ["Eaux", "Sols", "Air ambiant"],
        photo: photos.microscopes,
    },
    {
        number: "04",
        title: "Pharmaceutique et cosmétique",
        icon: BookOpen,
        examples: ["Stabilité", "Microbiologie", "Dosage d'actifs"],
        photo: photos.cells,
    },
];
export const faq = [
    {
        question: "La plateforme gère-t-elle plusieurs laboratoires ?",
        answer: "Oui. Chaque laboratoire est un tenant isolé, avec son schéma de données, ses utilisateurs, son catalogue d'essais et ses modèles de rapports.",
    },
    {
        question: "Comment les données sont-elles sécurisées ?",
        answer: "Authentification JWT, second facteur pour les rôles sensibles, permissions granulaires par action et journal d'audit immuable sur toutes les opérations sensibles.",
    },
    {
        question: "Quels rôles sont disponibles ?",
        answer: "Six rôles : client, personnel d'accueil, technicien, responsable de laboratoire, administrateur et super administrateur, chacun avec son espace dédié.",
    },
    {
        question: "Les rapports PDF sont-ils signés ?",
        answer: "Un rapport ne peut être diffusé qu'après validation technique puis signature électronique du responsable, horodatée et nominative.",
    },
    {
        question: "Le catalogue d'essais est-il personnalisable ?",
        answer: "Entièrement : méthodes, unités, seuils, incertitudes, délais et tarifs se configurent par laboratoire, sans développement.",
    },
    {
        question: "La facturation est-elle conforme au Maroc ?",
        answer: "Les devis et factures reprennent les mentions légales requises, la TVA applicable et une numérotation séquentielle par laboratoire.",
    },
];
export const cta = {
    title: "Prêt à moderniser votre laboratoire ?",
    description: "Déployez un LIMS complet, conforme et traçable, sans réécrire vos procédures.",
    button: "Intégrer mon laboratoire",
};
