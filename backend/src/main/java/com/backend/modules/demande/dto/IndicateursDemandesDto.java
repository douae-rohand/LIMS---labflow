package com.backend.modules.demande.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compteurs du laboratoire courant, alignés sur {@code StatutDemande}.
 * <ul>
 *   <li>{@code enAttente} : {@code SOUMISE}</li>
 *   <li>{@code acceptees} : {@code ACCEPTEE}</li>
 *   <li>{@code refusees} : {@code REJETEE}</li>
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
