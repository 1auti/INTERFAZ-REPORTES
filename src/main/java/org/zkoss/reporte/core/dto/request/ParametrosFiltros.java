package org.zkoss.reporte.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametrosFiltros {
    // Fechas
    private String fechaInicio;
    private String fechaFin;

    // Ubicación
    private String provincia;
    private String municipio;

    // Consolidación
    private Boolean consolidado;
    private List<String> consolidacion;
    private String periodoTemporal;

    // Paginación
    private Integer pagina;
    private Integer tamanoPagina;
}