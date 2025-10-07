package org.zkoss.reporte.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaQuery {
    private String nombreQuery;
    private ParametrosFiltros parametrosFiltros;
    private String formato;
}