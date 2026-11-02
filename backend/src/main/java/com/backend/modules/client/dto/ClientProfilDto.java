package com.backend.modules.client.dto;

import com.backend.modules.client.entity.TypeClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfilDto {
    private Long id;
    private Long utilisateurId;

    /** Nom de l'utilisateur (lecture seule ; non modifiable via le profil). */
    private String nom;
    /** Prénom de l'utilisateur (lecture seule ; non modifiable via le profil). */
    private String prenom;
    /** Email de connexion (lecture seule ; non modifiable via le profil). */
    private String email;
    /** Téléphone de l'utilisateur (lecture seule ; non modifiable via le profil). */
    private String telephone;

    private TypeClient typeClient;
    private String raisonSociale;
    private String ice;
    private String adresse;
    private Boolean consentementCndp;
    private Instant dateConsentementCndp;
    private String versionConsentementCndp;
    private Instant dateCreation;
}
