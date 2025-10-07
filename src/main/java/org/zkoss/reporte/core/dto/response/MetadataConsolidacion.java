package org.zkoss.reporte.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataConsolidacion {

    private Boolean consolidado;
    private List<String> camposAgrupacion;
    private List<String> camposNumericos;
    private String periodoTemporal;  // DIARIO, MENSUAL, etc
    private Integer registrosOriginales;  // Antes de consolidar
    private Integer registrosConsolidados;  // Después de consolidar

}
