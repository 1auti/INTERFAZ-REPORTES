package org.zkoss.reporte.core.service.interfaces;

import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.response.QueryResponse;

public interface ReporteService {

    /**
     * Ejecutar consulta por tipo
     */
    QueryResponse ejecutarConsulta(String tipoConsulta, ConsultaQuery consulta);

    /**
     * Descargar archivo de consulta
     */
    byte[] descargarConsulta(String tipoConsulta, ConsultaQuery consulta);

}
