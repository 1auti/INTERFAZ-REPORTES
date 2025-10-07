package org.zkoss.reporte.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataQuery {
    private String codigoQuery;
    private String nombreQuery;
    private String categoria;
    private LocalDateTime fechaEjecucion;
    private Long tiempoEjecucionMs;
    private String provincia;  // Si es multi-provincia
    private List<String> provinciasIncluidas;  // Para queries consolidadas
}
