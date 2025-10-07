package org.zkoss.reporte.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class MetadataPaginacion {
    private Integer paginaActual;
    private Integer registrosPorPagina;
    private Long totalRegistros;
    private Integer totalPaginas;
    private Boolean tieneSiguiente;
    private Boolean tieneAnterior;
    private Map<String, Object> lastKey;  // Para keyset pagination
}
