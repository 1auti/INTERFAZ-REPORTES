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
public class Query {
    private String codigo;
    private String nombre;
    private String descripcion;
    private String sqlQuery;
    private String categoria;
    private Boolean activa;
    private Boolean esConsolidable;
    private List<String> camposAgrupacion;
    private List<String> camposNumericos;
    private List<String> tags;
}