<!-- audience: responsable -->
# Guide du Responsable de laboratoire

## Le menu du Responsable
Le menu comprend : Tableau de suivi, Demandes clients, Planification, Validation résultats, Rapports, Stock (consultation), Employés, Catalogue, Exports.

## Accepter ou refuser une demande
Dans « Demandes clients », ouvrez une demande au statut « Nouveau ». Acceptez-la, ou refusez-la en indiquant un motif visible par le client. À l'acceptation, la facture est émise automatiquement.

## Affecter les analyses (planification)
Dans « Planification », affectez chaque analyse à un technicien. Trois conditions sont exigées : l'échantillon est conforme, la facture est au moins partiellement payée, et le technicien possède le domaine de compétence de l'analyse (Biochimie, Hématologie, Microbiologie, Environnement…). Les domaines des techniciens sont attribués par l'administrateur dans « Employés ». La demande passe alors « En cours ».

## Valider les résultats
Dans « Validation résultats », examinez chaque résultat saisi par un technicien. Trois décisions sont possibles : valider, retourner au technicien pour correction, ou rejeter (avec un motif). La validation finale doit être faite par une autre personne que celle qui a saisi le résultat.

## Signer le rapport
Quand toutes les analyses sont validées, signez le rapport dans « Rapports ». La demande passe « Terminée » et le client reçoit son rapport. Un rapport partiel peut être publié avec les seules analyses déjà validées.

## Score d'anomalie des résultats
Dans « Validation résultats », chaque résultat reçoit un score d'anomalie de 0 à 100, calculé automatiquement par LabFlow (sans IA générative, aucune donnée ne quitte le serveur). Le score vaut 100 pour une valeur critique (hors des seuils critiques de l'essai), de 50 à 90 pour une valeur hors de l'intervalle de référence (OOS, selon l'écart), et de 40 à 80 pour une valeur inhabituelle (OOT) : dans les normes mais très différente des résultats précédents du même essai au laboratoire (il faut au moins 10 résultats précédents). Une valeur inhabituelle peut révéler une erreur de saisie, d'unité, d'échantillon ou d'appareil. À partir de 40, le résultat est en alerte : il remonte dans la file (après les critiques et les hors norme), le filtre de priorité propose « Inhabituels », et l'explication du score apparaît dans le panneau « Examiner ». Le score aide à prioriser : il ne valide ni ne rejette rien, la décision reste la vôtre.

## Synthèse IA des résultats
Après la signature d'un rapport, LabFlow rédige automatiquement une synthèse des résultats, affichée au client à côté du rapport avec la mention « Synthèse automatique, non contractuelle ». Elle ne contient ni diagnostic ni traitement, et seules les valeurs anonymes des analyses sont envoyées au service d'IA (jamais le nom du patient). Dans « Rapports », le bouton « Synthèse IA » d'un rapport signé ou envoyé permet de la lire et, si elle n'a pas pu être générée, de la régénérer.

## Tableau de suivi
Le tableau de suivi donne une vue d'ensemble : demandes par statut, analyses en retard, charge des techniciens, et alertes anomalies (les résultats à valider dont le score d'anomalie est d'au moins 40, du plus élevé au plus bas).

## Exports
La page « Exports » permet d'exporter des données du laboratoire.
