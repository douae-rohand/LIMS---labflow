package com.backend.modules.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogueStatsDto {
    private Long totalEssais;
    private Long essaisActifs;
    private Long essaisInactifs;
    private Map<String, Long> essaisParDomaine;
}
