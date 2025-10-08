package org.zkoss.reporte.core.dto.response;

import java.time.LocalDateTime;


import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MetadataQueryRegistro{

    private Integer id;
    private String nombre;
    private String codigo;
    private String descripcion;
    private String sqlQuery;
    private String categoria;
    private Boolean activa;
    private Boolean esConsolidable;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualzacion;

    private MetadataConsolidacion metadataConsolidacion;





}