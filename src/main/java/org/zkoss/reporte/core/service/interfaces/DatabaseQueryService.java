package org.zkoss.reporte.core.service.interfaces;

import java.util.List;
import java.util.Map;

import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.request.Query;
import org.zkoss.reporte.core.dto.response.MetadataQueryRegistro;
import org.zkoss.reporte.core.dto.response.MetadataRegistroQuery;
import org.zkoss.reporte.core.dto.response.QueryResponse;

public interface DatabaseQueryService {

    MetadataRegistroQuery registrarQuery(Query query);
    List<MetadataQueryRegistro> traerQuerys(String categoria);
    MetadataQueryRegistro obtenerQuery(String codigo);
    MetadataRegistroQuery actualizarQuery(String codigo, Query query);
    Map<String, Object> eliminarQuery(String codigo);

    // Ejecución de queries
    QueryResponse ejecutarQuery(String codigo, ConsultaQuery consulta);
    QueryResponse ejecutarQueryConsolidada(String codigo, ConsultaQuery consulta);

    // Búsqueda y filtrado
    List<MetadataQueryRegistro> buscarQueries(String texto);
    List<MetadataQueryRegistro> obtenerQueriesConsolidables();
    List<MetadataQueryRegistro> obtenerQueriesPopulares(int limite);

    // Estadísticas
    Map<String, Object> obtenerEstadisticas();
    Map<String, Object> obtenerEstadisticasQuery(String codigo);

}