package com.backend.modules.ia.dto;

public enum TypeTacheIa {
    /** Analyse prédictive des résultats. */
    PREDICTION_RESULTAT,
    /** Détection d'anomalies dans les données. */
    DETECTION_ANOMALIE,
    /** Résumé automatique d'un rapport. */
    RESUME_RAPPORT,
    /** Recommandation d'analyses complémentaires. */
    RECOMMANDATION_ANALYSE,
    /** Question/Réponse sur la base documentaire. */
    QA_DOCUMENTAIRE
}
