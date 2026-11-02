package com.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Enveloppe des listes agrégées de l'espace client (tous laboratoires confondus).
 *
 * <p>{@code laboratoiresIndisponibles} compte les laboratoires du client dont la
 * lecture a échoué (schéma inaccessible, laboratoire suspendu). L'exception est
 * journalisée puis ignorée : elle ne casse jamais la liste, mais elle est
 * désormais comptée et exposée pour signaler une vue partielle.
 *
 * @param <T> type des éléments de la liste
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListeAgregeeDto<T> {

    private List<T> elements;

    /** Nombre de laboratoires du client dont la lecture a échoué. */
    private int laboratoiresIndisponibles;

    public static <T> ListeAgregeeDto<T> de(List<T> elements, int laboratoiresIndisponibles) {
        return ListeAgregeeDto.<T>builder()
                .elements(elements)
                .laboratoiresIndisponibles(laboratoiresIndisponibles)
                .build();
    }
}
