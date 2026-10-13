package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compteurs du laboratoire courant, alignés sur {@code StatutDemande}.
 * <ul>
 *   <li>{@code enAttente} : {@code NOUVEAU}</li>
 *   <li>{@code acceptees} : {@code ACCEPTEE}</li>
 *   <li>{@code refusees} : {@code REFUSEE}</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicateursDemandesDto {
    private long enAttente;
    private long acceptees;
    private long refusees;
    private long total;
}
