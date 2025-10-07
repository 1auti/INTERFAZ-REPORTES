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
public class Consolidacion {

    private Boolean esConsolidable;
    private List<String> camposAgrupacion;
    private List<String> camposNumericos;
    private List<String> camposUbicacion;
    private List<String> camposTiempo;
}
