package org.zkoss.reporte.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public  class ActividadItem {

    private String nombreQuery;
    private String fechaEjecucion;
    private String registros;

}
