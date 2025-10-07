package org.zkoss.reporte.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueryRegistration {

    private String nombreQuery;
    private String descripcion;
    private String categoria;
    private Boolean sincronico;

}
