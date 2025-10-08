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

    private Integer id;
    private String codigo;
    private String nombre;
    private String categoria;
    private String descripcion;
    private String sqlQuery;
    private Boolean activa;
    private Boolean esConsolidable;
    private String estado;
    private LocalDateTime fechaEjecucion;
    private Long tiempoEjecucionMs;
    private String provincia;  // Si es multi-provincia
    private List<String> provinciasIncluidas;  // Para queries consolidadas
    private List<String> camposAgrupacionList;
    private List<String> camposNumericosList;
    private List<String> camposUbicacionList;
    private List<String> camposTiempoList;
    private Long contadorUsos;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
