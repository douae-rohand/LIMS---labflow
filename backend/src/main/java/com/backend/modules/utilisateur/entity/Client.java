package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Client externe qui soumet des demandes d'analyse.
 * Table : {@code utilisateur_client}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "utilisateur_client")
@DiscriminatorValue("CLIENT")
public class Client extends Utilisateur {

    /** Raison sociale de l'organisation du client (optionnel). */
    @Column(name = "organisation", length = 200)
    private String organisation;

    /** Adresse postale du client. */
    @Column(name = "adresse", length = 500)
    private String adresse;

    /** Numéro SIRET ou équivalent (optionnel). */
    @Column(name = "numero_siret", length = 20)
    private String numeroSiret;
}
